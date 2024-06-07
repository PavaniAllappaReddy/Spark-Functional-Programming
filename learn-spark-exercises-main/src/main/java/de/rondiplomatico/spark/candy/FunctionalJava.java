package de.rondiplomatico.spark.candy;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.rondiplomatico.spark.candy.base.data.Candy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rondiplomatico.spark.candy.base.Utils;
import de.rondiplomatico.spark.candy.base.data.Color;
import de.rondiplomatico.spark.candy.base.data.Crush;
import de.rondiplomatico.spark.candy.base.data.Deco;

/**
 * Exercises for the first section of the course.
 *
 * Comprises basic functional operations using the java streaming api.
 *
 * @since 2022-06-22
 * @author wirtzd
 *
 */
@SuppressWarnings("java:S100")
public class FunctionalJava {

    private static final Logger log = LoggerFactory.getLogger(FunctionalJava.class);

    /**
     * Configure your environment to run this class for section 1.
     *
     * @param args
     */
    public static void main(String[] args) {
        /**
         * E1: Generate crushes
         */
         List<Crush> data = e1_crush(6000);
         log.info("provides value is: 6000");

        /**
         * E2: Filtering
         */
        e2_countCandies(data);

        /**
         * E3: Grouping
         */
         e3_countByColor(data);

        /**
         * E4: Lookups
         */
         e4_cityLookup(data);
    }

    /**
     * Creates a specified amount of candy crush events.
     *
     * @param n
     *            the number of desired crush events
     * @return the list of crush events
     */
    public static List<Crush> e1_crush(int n) {
        List<Crush> orders = generateRandomCrushEvents(n);
        log.info(orders.size() + " candies have been crushed!");
        log.info("Generated {} crush events.", orders.size());
        return orders;

    }

    public static List<Crush> generateRandomCrushEvents(int n) {
        Random random = new Random();
        //random.nextInt(100);
        return IntStream.range(0, n)
                .mapToObj(i -> new Crush(new Candy(Utils.randColor(),Utils.randDeco()), Utils.randUser(), Utils.randTime()) )
                .collect(Collectors.toList());
    }



    /**
     * Performs various counts on crushed candies
     *
     *
     * Useful links for further reference:
     *
     * Java Streaming API:
     *
     * @see https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html
     * @see https://www.baeldung.com/java-8-streams
     * @see https://stackify.com/streams-guide-java-8/
     *
     *      Lambdas:
     *
     * @see https://www.w3schools.com/java/java_lambda.asp
     * @see https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html
     *
     * @param data
     *            A list of crushes
     */
    public static void e2_countCandies(List<Crush> data) {
        /**
         * Answers the question "how many red striped candies have been crushed?"
         */
        long res = data.stream() // stream() converts a Java collection to a java stream
                .map(Crush::getCandy) // transforms the stream elements, here selecting the candy object of the crush
                .filter(c -> c.getColor().equals(Color.RED)) // Filters a stream by a specified predicate
                .filter(c -> c.getDeco().equals(Deco.HSTRIPES) || c.getDeco().equals(Deco.VSTRIPES))
                .count(); // Terminates the stream, processing all it's elements by counting them
        // Log the counted results.
        log.info("The crush data contains {} red striped candies!", res);

        /*
         * TODO E2: Filtering
         *
         * Count how many wrapped candies have been crushed between 12-13 o'clock and log the results like above.
         */

     LocalTime startTime = LocalTime.of(12, 0);
     LocalTime endTime = LocalTime.of(13, 0);

        long wrappedCandies = data.stream()
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
     * Performs various counts on the provided crush data
     *
     * @param data
     */
    public static void e3_countByColor(List<Crush> data) {
        /**
         * Classical, imperative implementation by using a map to
         * store the counts for each color.
         */
        Map<Color, Integer> res = new EnumMap<>(Color.class);
        for (Crush c : data) {
            Color col = c.getCandy().getColor();
            Integer count = res.get(col);
            if (count == null) {
                res.put(col, 1);
            } else {
                res.put(col, count + 1);
            }
        }
        res.forEach((c, i) -> log.info("The crush data contains {} {} candies", i, c));

        /*
         * TODO E3: Grouping
         *
         * Implement the same logic as above using the java streaming api.
         * Log your results and compare!
         *
         * Hints: The function "collect" with the "groupingBy" and downstream "counting" Collectors come in handy.
         */
        Map<Color, Long> colorCounts = data.stream()
                .collect(Collectors.groupingBy(c -> c.getCandy().getColor(), Collectors.counting()));

        colorCounts.forEach((c, i) -> log.info("The crush data contains {} {} candies (streaming)", i, c));


        /*
         * TODO E3: Grouping (Bonus question)
         *
         * Answer the question: "How many blue candies have been crushed per decoration type?"
         * Log your results.
         */
        Map<Deco, Long> blueCandyCounts = data.stream()
                .filter(c -> c.getCandy().getColor() == Color.BLUE)
                .collect(Collectors.groupingBy(c -> c.getCandy().getDeco(), Collectors.counting()));

        blueCandyCounts.forEach((dt, count) -> log.info("The crush data contains {} blue candies with {} decoration", count, dt));

    }

    /**
     * Computes some statistics for candy crushes considering the city the persons are living in
     *
     * @param data
     */
    public static void e4_cityLookup(List<Crush> data) {

        /**
         * Get the map of cities from Utils
         */
        Map<String, String> cities = Utils.getHomeCities();

        /**
         * Imperative implementation: How may crushes per city?
         */
        Map<String, Integer> counts = new HashMap<>();
        for (Crush c : data) {
            // Look up the city using the user as key
            String city = cities.get(c.getUser());
            // The "getOrDefault" allows to formulate the counting code more compact - compare e3_countByColor :-)
            counts.put(city, counts.getOrDefault(city, 0) + 1);
        }
        counts.forEach((c, i) -> log.info("There are {} crushes in {}", i, c));

        /*
         * TODO E4: Lookups
         *
         * Implement "How may crushes per city?" using streams, map lookup and collectors.
         * Log your results.
         */

        Map<String, Long> crushesPerCity = data.stream()
                .map(c -> cities.get(c.getUser()))
                .collect(Collectors.groupingBy(city -> city, Collectors.counting()));

        crushesPerCity.forEach((city, count) -> log.info("There are {} crushes in {}", count, city));


        /**
         * Teach-In: Demonstrating the requirement of "effectively final" fields
         */
        // cities = null;

        /*
         * TODO E4: Lookups (Bonus question)
         *
         * Implement "How many candies in Ismaning between 14-15 o'clock, counted by color?" with java streaming. Use what you have learned before to succeed.
         * Log your results.
         */
        Map<Color, Long> candiesInIsmaningByColor = data.stream()
                .filter(c -> "Ismaning".equals(cities.get(c.getUser())))
                .filter(c -> {
                    LocalTime crushTime = c.getTime();
                    return !crushTime.isBefore(LocalTime.of(14, 0)) && !crushTime.isAfter(LocalTime.of(15, 0));
                })
                .collect(Collectors.groupingBy(crush->crush.getCandy().getColor(), Collectors.counting()));

        candiesInIsmaningByColor.forEach((color, count) -> log.info("There are {} candies in Ismaning of color {}", count, color));

    }

}