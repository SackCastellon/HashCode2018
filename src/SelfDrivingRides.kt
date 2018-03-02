/*
 * Code created by Juan José González Abril (https://github.com/SackCastellon)
 * for team #LlorensOn10 for the Google Hash Code 2018
 */

import java.io.File
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.sign

val dataNames = listOf("a_example", "b_should_be_easy", "c_no_hurry", "d_metropolis", "e_high_bonus")

fun main(args: Array<String>) {
    for (name in dataNames) {
        val data = readData(File("./data/$name.in"))
        val results = compute(data)
        writeResult(results, File("./data/$name.out"))
    }
}

fun readData(inFile: File): DataSet {
    val lines = inFile.readLines()

    fun getRides(n: Int): List<Ride> {
        var i = 0
        return lines.drop(1).take(n).map {
            it.split(" ").map { it.toInt() }.let {
                Ride(i++, Point(it[0], it[1]), Point(it[2], it[3]), it[4], it[5])
            }
        }
    }

    return lines[0].split(" ").map { it.toInt() }.let {
        DataSet(it[0], it[1], it[2], getRides(it[3]), it[4], it[5])
    }
}

fun compute(data: DataSet): List<Vehicle> {
    val assignedRides = mutableSetOf<Int>()
    val vehiclesList = List(data.vehicles) { Vehicle() }

    for (t in 0 until data.steps) {
        vehiclesList.filter { !it.hasRide }.let { vehicles ->
            val sortedRides = data.rides
                .filter { !assignedRides.contains(it.i) }
                .flatMap { r -> vehicles.map { Pair(r, it) } }
                .sortedWith(
                    Comparator
                        .comparingInt<Pair<Ride, Vehicle>> { (r, v) -> ((r.earliestStart - t) - (v.location - r.start).steps).absoluteValue }
                        .thenComparingInt { (r, v) -> r.latestFinish - (t - r.steps - (v.location - r.start).steps) }
                )

            while (vehicles.any { !it.hasRide } && assignedRides.size != data.rides.size)
                sortedRides
                    .asSequence()
                    .filter { (r, v) -> !assignedRides.contains(r.i) && !v.hasRide }
                    .firstOrNull()
                    ?.let { (r, v) ->
                        v.assignRide(r)
                        assignedRides.add(r.i)
                    }
        }

        vehiclesList.forEach { it.move(t) }

        if (assignedRides.size == data.rides.size && vehiclesList.all { it.finished })
            break
    }

    return vehiclesList
}

fun writeResult(results: List<Vehicle>, outFile: File) {
    outFile.writeText(results.joinToString("\n") { it.ridesFinished.let { "${it.size} ${it.joinToString(" ")}" } })
}

data class DataSet(val rows: Int, val columns: Int, val vehicles: Int, val rides: List<Ride>, val bonus: Int, val steps: Int)

data class Ride(val i: Int, val start: Point, val end: Point, val earliestStart: Int, val latestFinish: Int) {
    val steps: Int = (start - end).steps
    override fun toString(): String = "Ride($i, $start -> $end, s=$earliestStart, f=$latestFinish)"
}

data class Point(val x: Int = 0, val y: Int = 0) {
    val steps: Int = x.absoluteValue + y.absoluteValue
    operator fun minus(that: Point): Point = Point((this.x - that.x), (this.y - that.y))
    override fun toString(): String = "[$x, $y]"
}

class Vehicle {

    val ridesFinished = mutableSetOf<Int>()

    fun assignRide(ride: Ride) {
        this.ride = ride
    }

    private fun clearRide() {
        ride = null
        rideStarted = false
    }

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
    val hasRide: Boolean get() = ride != null

    fun move(t: Int) {
        ride?.let {
            if (!rideStarted && it.start == location && t >= it.earliestStart)
                rideStarted = true
        }

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

        ride?.let {
            if (t < it.latestFinish) {
                if (it.end == location) {
                    ridesFinished.add(it.i)
                    clearRide()
                }
            } else {
                clearRide()
            }
        }
    }
}