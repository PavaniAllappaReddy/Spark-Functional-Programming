package de.rondiplomatico.spark.candy;

import de.rondiplomatico.spark.candy.base.SparkBase;
import de.rondiplomatico.spark.candy.base.Utils;
import de.rondiplomatico.spark.candy.base.data.Candy;
import de.rondiplomatico.spark.candy.base.data.Color;
import de.rondiplomatico.spark.candy.base.data.Crush;
import de.rondiplomatico.spark.candy.base.data.Deco;
import lombok.var;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Exercises for the basic spark section of the course.
 *
 * @since 2022-06-22
 * @author wirtzd
 *
 */
@SuppressWarnings("java:S100")
public class SparkBasics extends SparkBase {

    private static final Logger log = LoggerFactory.getLogger(SparkBasics.class);

    /**
     * Local field containing the cities map.
     * (Placed here for a specific demonstration around serializability and spark)
     */
    public static Map<String, String> cities = Utils.getHomeCities();

    /**
     * Configure your environment to run this class for section 2.
     *
     * @param args
     */
    public static void main(String[] args) {
        // Create a new instance of the SparkBasics exercise set.
        SparkBasics s = new SparkBasics();

        /**
         * E1: Generate crushes as RDD
         */
         JavaRDD<Crush> rdd = s.e1_crushRDD(60000);
        log.info("JavaRDD data is: " +rdd);

        /*
         * TODO E1: Log the number of partitions and elements in the created RDD.
         */

        /**
         * E2: Filtering
         */
         s.e2_countCandiesRDD(rdd);

        /**
         * E3: Grouping
         */
         s.e3_countByColorRDD(rdd);

        /**
         * E4: Lookup
         */
        s.e4_cityLookupRDD(rdd);


    }

    /**
     * Creates a RDD of n crushes
     *
     * @param n
     * @return the rdd
     */
    public JavaRDD<Crush> e1_crushRDD(int n) {

        /*
         * TODO E1: Create crush RDD
         *
         * Use the functions from FunctionalJava to create some crushes and parallelize them using the java spark context
         */
       SparkBasics sparkBasics = new SparkBasics();
       return sparkBasics.getJavaSparkContext().parallelize(FunctionalJava.e1_crush(n));

    }

    /**
     * Implements the various counting questions from {@link FunctionalJava} using spark
     *
     * @param crushes
     */
    public void e2_countCandiesRDD(JavaRDD<Crush> crushes) {
        /*
         * TODO E2: Filtering
         *
         * Implement "How many red striped candies have been crushed?"
         */
        long res =crushes// stream() converts a Java collection to a java stream
                .map(Crush::getCandy) // transforms the stream elements, here selecting the candy object of the crush
                .filter(c -> c.getColor().equals(Color.RED)) // Filters a stream by a specified predicate
                .filter(c -> c.getDeco().equals(Deco.HSTRIPES) || c.getDeco().equals(Deco.VSTRIPES))
                .count();

        log.info("The crush data contains {} red striped candies!", res);

        /*
         * TODO E2: Filtering
         *
         * Count how many wrapped candies have been crushed between 12-13 o'clock and log the results
         */
        LocalTime startTime = LocalTime.of(12, 0);
        LocalTime endTime = LocalTime.of(13, 0);

        long wrappedCandies = crushes
                .filter(crush -> {
                    LocalTime crushTime = crush.getTime();
                    return crushTime.isAfter(startTime) && crushTime.isBefore(endTime);
                })
                .map(Crush::getCandy)
                .filter(c -> c.getDeco().equals(Deco.WRAPPED))
                .count();

        log.info("The crush data contains {} wrapped candies crushed between 12-13 o'clock!", wrappedCandies);
       
    }

    /**
     * Performs various counts using spark
     *
     * @param crushes
     */
    public void e3_countByColorRDD(JavaRDD<Crush> crushes) {
        /*
         * TODO E3: Grouping
         * Implement FunctionalJava-E3 using Spark!
         * - How many Candies are crushed per color?
         * - Stick with the functional flow "group, count, collect"
         * - Log your results.
         *
         * Hint: Iterables::size is convenient should you need to count the number of elements of an iterator.
         */
        Map<Color, Long> colorCounts = crushes.map(Crush::getCandy).groupBy(Candy::getColor)
                .mapValues(Iterable::spliterator)
                .mapValues(Spliterator::getExactSizeIfKnown)
                .collectAsMap();

        // Logging the results
        colorCounts.forEach((color, count) -> log.info("The crush data contains candies  " + "Color: " + color + ", Count: " + count));

        /*
         * TODO E3: (Bonus question)
         *
         * Implement "How many blue candies have been crushed per decoration type?"
         * - Avoid the groupBy() transformation - explore what better functions are available on JavaPairRDD!
         * - Can you also simplify the implementation of the first question similarly?
         */

        /*
         * Fast variant: Let spark do the counting!
         */


        JavaPairRDD<Deco, Integer> decoCounts = crushes
                .map(Crush::getCandy)
                .filter(c -> c.getColor().equals(Color.BLUE))
                .mapToPair(c -> new Tuple2<>(c.getDeco(), 1))
                .reduceByKey(Integer::sum);

        Map<Deco, Integer> result = new EnumMap<>(Deco.class);
        result.putAll(decoCounts.collectAsMap());

        result.forEach((x, y) -> log.info("Blue Candies in {} are crushed {} times", x, y));

    }

    /**
     * Performs some statistics on crushes using spark and lookups.
     *
     * @param crushes
     */
    public void e4_cityLookupRDD(JavaRDD<Crush> crushes) {

        /*
         * TODO E4: Lookups
         *
         * - Understand this implementation of counting cities using a count map
         * - Run the method
         * - Investigate what might be going wrong here?
         */
        Map<String, Integer> counts = new HashMap<>();
        crushes.foreach(c -> {
            String city = Utils.getHomeCities().get(c.getUser());
            int newcnt = counts.getOrDefault(city, 0) + 1;
            counts.put(city, newcnt);
            log.info("Counting crush in {}, totalling {}", city, newcnt);
        });
        log.info("Counted crushes in {} cities", counts.size());
        counts.forEach((c, i) -> log.info("There are {} crushes in {}", i, c));

        /*
         * TODO E4: Lookups
         *
         * Implement the counting as result of the transformation, using
         * - the class field "cities" as lookup (similar to FJ-E4)
         * - countByValue() as action
         * - Log your results
         * - Run the code
         */
        

        /*
         * TODO E4: Lookups
         * How many candies in Ismaning between 14-15 o'clock, counted by color?
         * Use all you've learned before.
         */
        
    }

}
