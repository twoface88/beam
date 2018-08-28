package beam.analysis.plot.graph;

import beam.analysis.plots.RealizedModeStats;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static beam.analysis.plot.graph.GraphTestRealizedUtil.createDummySimWithCRCXML;
import static org.junit.Assert.assertEquals;

public class RealizedModeGraphTest {
    private RealizedModeStats realizedModeStats = new RealizedModeStats();
    private static final Logger log = LoggerFactory.getLogger(RealizedModeGraphTest.class);


    @BeforeClass
    public static void setUpCRC() {
        createDummySimWithCRCXML();
    }

    @Test
    public void testShouldPassShouldReturnModeChoseEventOccurrenceForCRCUnitHour() {

        int expectedWalkResult = 2;
        int expectedCarResult = 2;
        int expectedRideHailResult = 2;
        int expectedOtherResult = 4;
        int hour = 6;

        Map<Integer,Map<String,Integer>> data = realizedModeStats.getHoursDataCountOccurrenceAgainstMode( );
        int actaulWalkResult = data.get(hour).get(GraphTestRealizedUtil.WALK);
        int actaulCarResult = data.get(hour).get(GraphTestRealizedUtil.CAR);
        int actaulRideHailResult = data.get(hour).get(GraphTestRealizedUtil.RIDE_HAIL);
        int actaulOtherResult = data.get(hour).get(GraphTestRealizedUtil.OTHERS);

        log.error("actual walk test 1-  "+actaulWalkResult);
        log.error("actual car test 1-  " + actaulCarResult);
        log.error("actual ride_hail test 1- " + actaulRideHailResult);
        log.error("actal others test 1- " + actaulOtherResult);

        assertEquals(expectedWalkResult,actaulWalkResult );
        assertEquals(expectedCarResult,actaulCarResult );
        assertEquals(expectedRideHailResult,actaulRideHailResult );
        assertEquals(expectedOtherResult,actaulOtherResult);

    }

    @Test
    public void testShouldPassShouldReturnModeChoseEventOccurrenceForCRCRCUnitHour() {

        int expectedWalkResult = 2;
        int expectedCarResult = 2;
        int expectedRideHailResult = 2;
        int expectedOtherResult = 4;
        int hour = 7;

        Map<Integer, Map<String,Integer>> data1 = realizedModeStats.getHoursDataCountOccurrenceAgainstMode( );
        int actaulWalkResult = data1.get(hour).get(GraphTestRealizedUtil.WALK);
        int actaulCarResult = data1.get(hour).get(GraphTestRealizedUtil.CAR);
        int actaulRideHailResult = data1.get(hour).get(GraphTestRealizedUtil.RIDE_HAIL);
        int actaulOtherResult = data1.get(hour).get(GraphTestRealizedUtil.OTHERS);

        log.error("actual walk test 2- "+actaulWalkResult);
        log.error("actual car test 2- " + actaulCarResult);
        log.error("actual ride_hail test 2- " + actaulRideHailResult);
        log.error("actal others test 2- " + actaulOtherResult);

        assertEquals(expectedWalkResult,actaulWalkResult );
        assertEquals(expectedCarResult,actaulCarResult );
        assertEquals(expectedRideHailResult,actaulRideHailResult );
        assertEquals(expectedOtherResult,actaulOtherResult);

    }

    @Test
    public void testShouldPassShouldReturnModeChoseEventOccurrenceForNestedCRCRCUnitHour() {

        int expectedWalkResult = 2;
        int expectedCarResult = 2;
        int expectedRideHailResult = 3;
        int expectedOtherResult = 3;
        int hour = 8;

        Map<Integer, Map<String,Integer>> data2 = realizedModeStats.getHoursDataCountOccurrenceAgainstMode( );
        int actaulWalkResult = data2.get(hour).get(GraphTestRealizedUtil.WALK);
        int actaulCarResult = data2.get(hour).get(GraphTestRealizedUtil.CAR);
        int actaulRideHailResult = data2.get(hour).get(GraphTestRealizedUtil.RIDE_HAIL);
        int actaulOtherResult = data2.get(hour).get(GraphTestRealizedUtil.OTHERS);

        log.error("actual walk test 3- "+actaulWalkResult);
        log.error("actual car test 3-  " + actaulCarResult);
        log.error("actual ride_hail test 3- " + actaulRideHailResult);
        log.error("actal others test 3- " + actaulOtherResult);
        assertEquals(expectedWalkResult,actaulWalkResult );
        assertEquals(expectedCarResult,actaulCarResult );
        assertEquals(expectedRideHailResult,actaulRideHailResult );
        assertEquals(expectedOtherResult,actaulOtherResult);

    }

    @Test // when replanning and upper mode choice are in same hour
    public void testShouldPassShouldReturnModeChoseEventOccurrenceForCRCForDifferentHoursTypeA() {

        int expectedWalkResult = 2;
        int expectedCarResult = 2;
        int expectedRideHailResult = 2;
        int expectedOtherResult = 4;

        Map<Integer, Map<String,Integer>> data3 = realizedModeStats.getHoursDataCountOccurrenceAgainstMode( );
        int actaulCarResult = data3.get(9).get(GraphTestRealizedUtil.CAR);
        int actaulRideHailResult = data3.get(9).get(GraphTestRealizedUtil.RIDE_HAIL);
        int actaulOtherResult = data3.get(9).get(GraphTestRealizedUtil.OTHERS);
        int actaulWalkResult = data3.get(10).get(GraphTestRealizedUtil.WALK);

        log.error("actual walk test 4- "+actaulWalkResult);
        log.error("actual car test 4-  " + actaulCarResult);
        log.error("actual ride_hail test 4- " + actaulRideHailResult);
        log.error("actal others test 4-  " + actaulOtherResult);

        assertEquals(expectedWalkResult,actaulWalkResult );
        assertEquals(expectedCarResult,actaulCarResult );
        assertEquals(expectedRideHailResult,actaulRideHailResult );
        assertEquals(expectedOtherResult,actaulOtherResult);

    }

    @Test // when replanning and lower mode choice are in same hour
    public void testShouldPassShouldReturnModeChoseEventOccurrenceForCRCForDifferentHoursTypeB() {

        int expectedWalkResult = 2;
        int expectedCarResult = 2;
        int expectedRideHailResult = 2;
        int expectedOtherResult = 4;

        Map<Integer, Map<String, Integer>> data3 = realizedModeStats.getHoursDataCountOccurrenceAgainstMode();
        int actaulCarResult = data3.get(11).get(GraphTestRealizedUtil.CAR);
        int actaulRideHailResult = data3.get(11).get(GraphTestRealizedUtil.RIDE_HAIL);
        int actaulOtherResult = data3.get(12).get(GraphTestRealizedUtil.OTHERS);
        int actaulWalkResult = data3.get(12).get(GraphTestRealizedUtil.WALK);

        log.error("actual walk test 5- "+actaulWalkResult);
        log.error("actual car test 5-  " + actaulCarResult);
        log.error("actual ride_hail test 5- " + actaulRideHailResult);
        log.error("actal others test 5- " + actaulOtherResult);
        log.debug("actual other " + actaulOtherResult);

        assertEquals(expectedWalkResult, actaulWalkResult);
        assertEquals(expectedCarResult, actaulCarResult);
        assertEquals(expectedRideHailResult, actaulRideHailResult);
        assertEquals(expectedOtherResult, actaulOtherResult);

    }
}
