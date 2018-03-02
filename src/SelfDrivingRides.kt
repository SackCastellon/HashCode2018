/*
 * Code created by Juan José González Abril (https://github.com/SackCastellon)
 * for team #LlorensOn10 for the Google Hash Code 2018
 */

import java.io.File
import kotlin.math.absoluteValue
import kotlin.math.sign

val dataNames = listOf("a_example", "b_should_be_easy", "c_no_hurry", "d_metropolis", "e_high_bonus")

fun main(args: Array<String>) {
    for (name in dataNames) {
        val data = readData(File("./$name.in"))
        val results = compute(data)
        writeResult(results, File("./$name.out"))
    }
}

fun readData(inFile: File): DataSet {
    val lines = inFile.readLines()

    fun getRides(n: Int): List<Ride> {
        return lines.drop(1).take(n).map {
            it.split(" ").map { it.toInt() }.let {
                Ride(Point(it[0], it[1]), Point(it[2], it[3]), it[4], it[5])
            }
        }
    }

    return lines[0].split(" ").map { it.toInt() }.let {
        DataSet(it[0], it[1], it[2], getRides(it[3]), it[4], it[5])
    }
}

fun compute(data: DataSet): List<Vehicle> {
    val rides = data.rides
    val assignedRides = mutableSetOf<Int>()

    val vehicles = List(data.vehicles) { Vehicle() }

    for (t in 0 until data.steps) {
        vehicles.forEach { v ->
            if (v.finished)
                rides.filterIndexed { index, _ -> !assignedRides.contains(index) }
                    .minWith(Comparator.comparing<Ride, Int> { ((t - it.earliestStart) - (v.location - it.start).steps).absoluteValue }.thenComparing<Int> { t - it.earliestStart }.thenComparing<Int> { (v.location - it.start).steps })
                    ?.let { ride ->
                        val i = rides.indexOf(ride)
                        assignedRides.add(i)
                        v.assignRide(ride, i)
                    }
            v.move(t)
        }

        if (assignedRides.size == rides.size)
            break
    }

    return vehicles
}

fun writeResult(results: List<Vehicle>, outFile: File) {
    outFile.writeText(results.joinToString("\n") { it.ridesFinished.let { "${it.size} ${it.joinToString(" ")}" } })
}

data class DataSet(val rows: Int, val columns: Int, val vehicles: Int, val rides: List<Ride>, val bonus: Int, val steps: Int)

data class Ride(val start: Point, val end: Point, val earliestStart: Int, val latestFinish: Int) {
    val steps: Int = (start - end).steps
}

data class Point(val x: Int = 0, val y: Int = 0) {
    val steps: Int = x.absoluteValue + y.absoluteValue

    operator fun minus(that: Point): Point = Point((this.x - that.x), (this.y - that.y))
}

class Vehicle {

    val ridesFinished = mutableSetOf<Int>()

    fun assignRide(ride: Ride, i: Int) {
        this.ride = ride
        this.rideIndex = i
    }

    private fun clearRide() {
        ride = null
        rideIndex = -1
    }

    private var rideIndex = -1

    private var ride: Ride? = null
        set(value) {
            if (value != null && field != null && !finished) error("Ride not finished, cannot assign another one.")
            field = value
        }

    private var rideStarted: Boolean = false

    private val destination: Point
        get() = ride?.let { if (rideStarted) it.end else it.start } ?: location


    var location: Point = Point()
        private set

    val finished: Boolean get() = ride.let { it == null || location == it.end }

    fun move(t: Int) {
        if (!rideStarted && ride?.let { it.start == location && it.earliestStart <= t } == true)
            rideStarted = true

        (destination - location).takeUnless { it.steps == 0 }?.let {
            var x = location.x
            var y = location.y

            if (it.x.sign != 0) {
                x += it.x.sign
            } else if (it.y.sign != 0) {
                y += it.y.sign
            }

            val p = Point(x, y)
            location = p
        }

        if (ride?.end == location) {
            ridesFinished.add(rideIndex)
            clearRide()
        } else if (ride?.let { it.end != location && t >= it.latestFinish } == true) {
            clearRide()
        }
    }
}

