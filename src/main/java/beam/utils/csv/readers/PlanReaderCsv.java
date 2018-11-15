package beam.utils.csv.readers;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.households.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;


public class PlanReaderCsv {

    private Logger log = LoggerFactory.getLogger(PlanReaderCsv.class);

    public String delimiter = ",";
    public static final String path = "test/input/beamville/test-data/";
    public static final String plansInputFileName = "plans-input.csv.gz";
    public static final String plansOutputFileName = "plans-output.xml";


    Map<String, Map<String, String>> buildings;
    Map<String, Map<String, String>> houseHolds;
    Map<String, Map<String, String>> parcel_attr;
    Map<String, Map<String, String>> persons;
    Map<String, Map<String, String>> units;


    public static void main(String[] args) throws IOException {

        PlanReaderCsv planReader = new PlanReaderCsv();

        planReader.readGzipScenario();
    }

    public PlanReaderCsv(){

        this(null);
    }

    public PlanReaderCsv(String delimiter) {

        this.delimiter = delimiter == null ? this.delimiter : delimiter;

    }

    public Population readPlansFromCSV(String plansFile) throws IOException{

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());

        BufferedReader reader;

        if(plansFile.endsWith(".gz")) {
            GZIPInputStream gzipStream = new GZIPInputStream(new FileInputStream(plansFile));

            reader = new BufferedReader(new InputStreamReader(gzipStream));
        }else {
            reader = new BufferedReader(new FileReader(plansFile));
        }
        String line = "";
        int idx = 0;

        while((line = reader.readLine()) != null){

            if(idx == 0) { idx++; continue; }
            String[] dRow = line.split(delimiter, -1);

            String personId = dRow[0];
            String planElement = dRow[1];
            String planElementId = dRow[2];
            String activityType = dRow[3];
            String x = dRow[4];
            String y = dRow[5];
            String endTime = dRow[6];
            String mode = dRow[7];

            Plan plan = null;
            Id<Person> _personId = Id.createPersonId(personId);
            if(!population.getPersons().keySet().contains(_personId)) {

                Person person = population.getFactory().createPerson(_personId);
                plan = population.getFactory().createPlan();
                plan.setPerson(person);
                person.addPlan(plan);
                person.setSelectedPlan(plan);
                population.addPerson(person);
            }else{
                Person person = population.getPersons().get(_personId);
                plan = person.getSelectedPlan();
            }


            if(planElement.equalsIgnoreCase("leg")){
                PopulationUtils.createAndAddLeg(plan, mode);
            }else if(planElement.equalsIgnoreCase("activity")){
                Coord coord = new Coord(Double.parseDouble(x), Double.parseDouble(y));
                Activity act = PopulationUtils.createAndAddActivityFromCoord(plan, activityType, coord);

                if(!endTime.isEmpty())
                    act.setEndTime(Double.parseDouble(endTime));
            }


            //printRow(dRow);
            idx++;
        }

        return population;
    }

    Population population = null;

    public Population readPlansFromCSV(BufferedReader reader) throws IOException{

        if(population == null) {
            population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        }

        String line = "";
        int idx = 0;

        while((line = reader.readLine()) != null){

            if(idx == 0) { idx++; continue; }
            String[] dRow = line.split(delimiter, -1);

            String personId = dRow[0];
            String planElement = dRow[1];
            String planElementId = dRow[2];
            String activityType = dRow[3];
            String x = dRow[4];
            String y = dRow[5];
            String endTime = dRow[6];
            String mode = dRow[7];

            Plan plan = null;
            Id<Person> _personId = Id.createPersonId(personId);
            if(!population.getPersons().keySet().contains(_personId)) {

                Person person = population.getFactory().createPerson(_personId);
                plan = population.getFactory().createPlan();
                plan.setPerson(person);
                person.addPlan(plan);
                person.setSelectedPlan(plan);
                population.addPerson(person);
            }else{
                Person person = population.getPersons().get(_personId);
                plan = person.getSelectedPlan();
            }




            if(planElement.equalsIgnoreCase("leg")){
                PopulationUtils.createAndAddLeg(plan, mode);
            }else if(planElement.equalsIgnoreCase("activity")){
                Coord coord = new Coord(Double.parseDouble(x), Double.parseDouble(y));
                Activity act = PopulationUtils.createAndAddActivityFromCoord(plan, activityType, coord);

                if(!endTime.isEmpty())
                    act.setEndTime(Double.parseDouble(endTime));
            }


            //printRow(dRow);
            idx++;
        }

        return population;
    }

    public void writePlansToXml(Population population, String outputFile) {
        new PopulationWriter(population).write(outputFile);

        log.info("Written plans successfully to {}", outputFile);
    }

    public void processPlans(BufferedReader reader) throws IOException {
        Population population = readPlansFromCSV(reader);


        writePlansToXml(population, path + plansOutputFileName);
    }

    public void printRow(String[] dRow){
        log.info("personId => {}, planElement => {} , planElementId => {} , activityType => {} , " +
                        "x => {} , y => {} , endTime => {} , mode => {}",
                dRow);
    }


    public void readGzipScenario(){

        CsvToMap csvToMap = new CsvToMap();

        TarArchiveInputStream tarInput = null;
        try {
            tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(path + "/urbansim.tar.gz")));

            TarArchiveEntry currentEntry = tarInput.getNextTarEntry();
            BufferedReader br = null;
            StringBuilder sb = new StringBuilder();
            while (currentEntry != null) {
                br = new BufferedReader(new InputStreamReader(tarInput)); // Read directly from tarInput

                System.out.println("For File = " + currentEntry.getName());

                switch (currentEntry.getName()){
                    case "buildings.csv":
                        buildings = csvToMap.read(br);
                        //printMap("buildings", buildings);
                        break;
                    case "households.csv":
                        houseHolds = csvToMap.read(br);
                        //printMap("houseHolds", houseHolds);



                        break;
                    case "parcel_attr.csv":
                        parcel_attr = csvToMap.read(br);
                        //printMap("parcel_attr", parcel_attr);
                        break;
                    case "persons.csv":
                        persons = csvToMap.read(br);
                        //printMap("persons", persons);
                        break;
                    case "plans.csv":
                        processPlans(br);
                        break;
                    case "units.csv":
                        units = csvToMap.read(br);
                        //printMap("units", units);
                        break;
                    default:
                        //printLines(br);
                }

                currentEntry = tarInput.getNextTarEntry(); // You forgot to iterate to the next file
            }

            processData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void printMap(String mapName, Map<String, Map<String, String>> map){

        System.out.println("=> => Map << "+ mapName + ">>");
        for(String id : map.keySet()){
            System.out.println("id -> " + id);
            System.out.println(map.get(id));

        }
    }

    private void printLines(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println("line="+line);
        }
    }

    Map<String, List<Id<Person>>> houseHoldPersons = new HashMap<>();


    private List<Id<Vehicle>> buildVehicles(int numberOfVehicles){


        List<Id<Vehicle>> vehicles = new ArrayList<>();

        for(int i=0; i< numberOfVehicles; i++){

            int vehicleId = vehicleCounter + 1;
            Id<Vehicle> vid = Id.createVehicleId(vehicleId);

            VehicleType vehicleType = new VehicleTypeImpl(Id.create(1, VehicleType.class));

            vehicles.add(vid);
            allVehicles.add(VehicleUtils.getFactory().createVehicle(vid, vehicleType));
        }


        return vehicles;
    }

    private List<Id<Person>> buildPersons(int numberOfPersons){

        List<Id<Person>> persons = new ArrayList<>();

        for(int i=0; i< numberOfPersons; i++){

            int personId = personCounter + 1;
            Id<Person> pid = Id.createPersonId(personId);
            Person person = population.getFactory().createPerson(pid);

            persons.add(pid);
            allPersons.add(person);
        }


        return persons;
    }

    private void processData(){


        for(String houseHoldId : houseHolds.keySet()){

            Map<String, String> houseHoldMap = houseHolds.get(houseHoldId);

            String houseHoldUnitId = houseHoldMap.get("unit_id");
            Map<String, String> unit = units.get(houseHoldUnitId);

            String buildingId = unit.get("building_id");
            Map<String, String> building = buildings.get(buildingId);
            String parcelId = building.get("parcel_id");
            Map<String, String> parcel = parcel_attr.get(parcelId);
            String x = parcel.get("x");
            String y = parcel.get("y");

            String hhId = houseHoldMap.get("household_id");
            Integer numberOfVehicles = Integer.parseInt(houseHoldMap.get("cars"));
            Integer numberOfPersons = Integer.parseInt(houseHoldMap.get("person"));

            Id<Household> _houseHoldId = Id.create(hhId, Household.class);
            HouseholdImpl objHouseHold = new HouseholdsFactoryImpl().createHousehold(_houseHoldId);

            List<Id<Vehicle>> vehicleIds = buildVehicles(numberOfVehicles);
            List<Id<Person>> personIds = buildPersons(numberOfPersons);

            Double _income = Double.parseDouble(houseHoldMap.get("income"));

            Income income = new IncomeImpl(_income, Income.IncomePeriod.year);
            objHouseHold.setVehicleIds(vehicleIds);
            objHouseHold.setMemberIds(personIds);
            objHouseHold.setIncome(income);

            houseHoldsList.add(objHouseHold);

            // PopulationImpl to set households
            // I have to check in Matsim
            /*
            Population will have
            plans, households, household will have persons and vehicles right
            create default car based on the number in the cars column in the households.csv file


             */

        }



    }


    public List<Person> getAllPersons() {
        return allPersons;
    }

    public List<Vehicle> getAllVehicles() {
        return allVehicles;
    }

    public List<Household> getHouseHoldsList() {
        return houseHoldsList;
    }

    List<Household> houseHoldsList = new ArrayList<>();

    List<Vehicle> allVehicles;
    List<Person> allPersons;
    int vehicleCounter = 0;
    int personCounter = 0;
}

