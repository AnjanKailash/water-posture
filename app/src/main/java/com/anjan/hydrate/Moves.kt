package com.anjan.hydrate

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class Category(val emoji: String, val label: String) {
    STRENGTH("💪", "Strength"),
    STRETCH("🧘", "Stretch"),
    MIND("🧠", "Mind & coordination"),
    EYES("👀", "Eyes"),
    BREATH("🌬️", "Breathing"),
    WALK("🚶", "Walk")
}

/** One quick activity suggested alongside "drink water". */
data class Move(
    val id: String,
    val emoji: String,
    val name: String,
    val category: Category,
    val summary: String,
    val steps: List<String>,
    val seconds: Int = 0,          // > 0 shows a countdown timer on the how-to page
    val intense: Boolean = false,  // avoided right after lunch in Smart picks
    val afterLunch: Boolean = false
)

object Moves {
    val all: List<Move> = listOf(
        // Strength
        Move("pushups", "💪", "10 Push-ups", Category.STRENGTH,
            "Floor, knees or desk push-ups — your choice.",
            listOf("Hands slightly wider than shoulders.",
                "Body in a straight line from head to heels (or knees).",
                "Lower your chest, then push back up. Repeat 10 times.",
                "Too hard? Push against your desk or a wall."), intense = true),
        Move("squats", "🦵", "15 Squats", Category.STRENGTH,
            "Stand up, sit back like into a chair, repeat.",
            listOf("Feet shoulder-width apart, arms forward.",
                "Push hips back and bend knees as if sitting on a chair.",
                "Keep chest up and heels down, then stand tall.",
                "Repeat 15 times at a steady pace."), intense = true),
        Move("situps", "🔥", "15 Crunches", Category.STRENGTH,
            "Quick core work on a mat or carpet.",
            listOf("Lie on your back, knees bent, feet flat.",
                "Hands lightly behind your head — don't pull your neck.",
                "Lift shoulders off the floor, pause, lower slowly.",
                "Repeat 15 times."), intense = true),
        Move("plank", "🧱", "30-second Plank", Category.STRENGTH,
            "Hold a straight line on forearms.",
            listOf("Forearms on the floor, elbows under shoulders.",
                "Straighten legs, body in one line, squeeze your stomach.",
                "Breathe normally and hold.",
                "Easier: knees down. Harder: hold 45 seconds."), seconds = 30, intense = true),
        Move("wallsit", "🪑", "30-second Wall sit", Category.STRENGTH,
            "The 'invisible chair' against a wall.",
            listOf("Back flat against a wall.",
                "Slide down until knees are bent (aim for 90°).",
                "Knees over ankles, arms relaxed.",
                "Hold, then stand up slowly."), seconds = 30, intense = true),
        Move("lunges", "🏃", "10 Lunges each leg", Category.STRENGTH,
            "Step forward, lower, push back.",
            listOf("Stand tall, step one foot forward.",
                "Lower until both knees are about 90°.",
                "Push through the front heel to come back.",
                "10 on each leg, alternating."), intense = true),
        Move("calves", "🦶", "20 Calf raises", Category.STRENGTH,
            "Easy to do right at your desk.",
            listOf("Stand holding your desk or chair for balance.",
                "Rise up onto your toes, pause one second.",
                "Lower slowly. Repeat 20 times.")),
        Move("deskpush", "🖥️", "10 Desk push-ups", Category.STRENGTH,
            "Office-friendly push-ups against your desk.",
            listOf("Hands on the edge of a sturdy desk, shoulder-width.",
                "Walk feet back until body is in a straight line.",
                "Bend elbows to bring chest to the desk, push back.",
                "Repeat 10 times.")),

        // Stretch
        Move("neck", "🙆", "Neck rolls", Category.STRETCH,
            "Release tension from screen time.",
            listOf("Sit tall, shoulders relaxed.",
                "Slowly roll your head in a circle, 5 times one way.",
                "Then 5 times the other way.",
                "Move gently — never force it.")),
        Move("shoulders", "🤷", "Shoulder rolls", Category.STRETCH,
            "Undo the hunched-over-laptop posture.",
            listOf("Arms hanging loose.",
                "Roll shoulders up, back and down — 10 times.",
                "Then 10 times forward.",
                "Finish by squeezing shoulder blades together for 5 seconds.")),
        Move("chest", "🚪", "Doorway chest stretch", Category.STRETCH,
            "Open up the chest after sitting.",
            listOf("Stand in a doorway, forearms on the frame at shoulder height.",
                "Step one foot forward until you feel a stretch across your chest.",
                "Hold and breathe slowly."), seconds = 30),
        Move("hamstring", "🦿", "Hamstring stretch", Category.STRETCH,
            "Loosen the backs of your legs.",
            listOf("Put one heel on a low step or chair, leg straight.",
                "Keep your back straight and lean forward from the hips.",
                "Hold 15 seconds, then switch legs."), seconds = 30),
        Move("wrists", "✋", "Wrist & finger stretch", Category.STRETCH,
            "Great after lots of typing.",
            listOf("Arm straight in front, palm up.",
                "With the other hand, gently pull fingers back — 10 seconds.",
                "Turn palm down and press the back of the hand — 10 seconds.",
                "Switch hands. Finish by spreading fingers wide 5 times.")),
        Move("twist", "🌀", "Seated spinal twist", Category.STRETCH,
            "Gentle twist without leaving your chair.",
            listOf("Sit tall, feet flat.",
                "Turn your upper body to the right, hand on the chair back.",
                "Hold 15 seconds, breathe, then switch sides."), seconds = 30),

        // Mind & coordination
        Move("fingertap", "🖐️", "Finger tapping race", Category.MIND,
            "Thumb touches each finger — then mix up the hands.",
            listOf("Touch your thumb to each finger: index → middle → ring → little, and back.",
                "Do it with both hands together, 3 times.",
                "Now the tricky part: left hand goes forward while right hand goes backward.",
                "Speed up as you get better!")),
        Move("thumbpinky", "👍", "Thumb–pinky switch", Category.MIND,
            "A classic brain-coordination drill.",
            listOf("Make two fists in front of you.",
                "Left hand: thumb up. Right hand: little finger out.",
                "Switch at the same time: left little finger, right thumb.",
                "Keep switching 10 times, as fast as you can.")),
        Move("circlesquare", "🔵", "Circle & square", Category.MIND,
            "Draw two different shapes at once.",
            listOf("Right hand draws circles in the air.",
                "At the same time, left hand draws squares.",
                "Keep going for 20 seconds, then swap hands."), seconds = 20),
        Move("noseear", "👃", "Nose & ear switch", Category.MIND,
            "Cross-body coordination with a clap.",
            listOf("Left hand touches your nose, right hand holds your left ear.",
                "Clap once.",
                "Switch: right hand on nose, left hand on right ear.",
                "Repeat 10 times, getting faster.")),
        Move("countback", "🔢", "Count back by 7", Category.MIND,
            "A 1-minute focus reset.",
            listOf("Start at 100.",
                "Count down by 7: 93, 86, 79 …",
                "Go as far as you can without stopping.",
                "Got to 2? Try starting from 200!"), seconds = 60),

        // Eyes
        Move("eyes", "👀", "20-20-20 eye break", Category.EYES,
            "Rest your eyes from the screen.",
            listOf("Look at something about 20 feet (6 m) away.",
                "Keep looking for 20 seconds.",
                "Blink slowly a few times before going back to the screen."), seconds = 20),

        // Breathing
        Move("boxbreath", "🌬️", "Box breathing", Category.BREATH,
            "Calm down in one minute.",
            listOf("Breathe in for 4 seconds.",
                "Hold for 4 seconds.",
                "Breathe out for 4 seconds.",
                "Hold for 4 seconds. Repeat 4 rounds."), seconds = 64),

        // Walk
        Move("walk2", "🚶", "2-minute walk", Category.WALK,
            "Walk around — refill your water bottle on the way.",
            listOf("Stand up and walk around the office or home.",
                "Take the stairs if you can.",
                "Refill your water bottle while you're up."), seconds = 120),
        Move("lunchwalk", "🌳", "Walk after lunch (15–30 min)", Category.WALK,
            "A post-lunch walk helps digestion and the afternoon slump.",
            listOf("Head out for an easy walk — 15 minutes minimum, 30 if you can.",
                "Comfortable pace; you should still be able to talk.",
                "Outside light is a bonus for energy."), afterLunch = true)
    )

    fun byId(id: String?): Move? = all.firstOrNull { it.id == id }
}

/**
 * Chooses the activity for each reminder.
 * Smart picks: whole library, shaped by time of day and variety.
 * My picks: only the activities the user ticked, still varied.
 */
object Recommender {

    private fun dayKey(t: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(t))

    fun current(c: Context): Move? = Moves.byId(Prefs.currentMove(c))

    fun pickNext(c: Context, now: Long = System.currentTimeMillis()): Move {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val sinceLunch = minuteOfDay - Prefs.lunchMinutes(c)
        val smart = Prefs.moveMode(c) == Prefs.MOVE_SMART

        val pool = (if (smart) Moves.all else Moves.all.filter { it.id in Prefs.selectedMoves(c) })
            .ifEmpty { Moves.all }

        // 1) First reminder in the 90 minutes after lunch: suggest the lunch walk (once a day).
        val today = dayKey(now)
        val lunchWalk = pool.firstOrNull { it.afterLunch }
        if (lunchWalk != null && sinceLunch in 0..90 && Prefs.lunchWalkDay(c) != today) {
            Prefs.setLunchWalkDay(c, today)
            return remember(c, lunchWalk)
        }

        // 2) Otherwise a weighted random pick that favours variety.
        val candidates = pool.filter { !it.afterLunch }.ifEmpty { pool }
        val recent = Prefs.recentMoves(c)
        val lastCategory = Moves.byId(recent.lastOrNull())?.category

        fun weight(m: Move): Double {
            var w = 1.0
            if (m.id in recent) w *= 0.15                     // not again so soon
            if (m.category == lastCategory) w *= 0.4          // switch it up
            if (smart) {
                if (sinceLunch in 0..60 && m.intense) w *= 0.1                     // go easy after eating
                if (minuteOfDay < 11 * 60 && m.category == Category.STRETCH) w *= 1.8  // morning mobility
                if (minuteOfDay in (15 * 60)..(17 * 60) &&
                    (m.category == Category.STRENGTH || m.category == Category.WALK)) w *= 1.6  // afternoon slump
                if (minuteOfDay >= 19 * 60 && m.intense) w *= 0.5                 // calmer evenings
            }
            return w
        }

        val weights = candidates.map { weight(it) }
        var r = Random.nextDouble() * weights.sum()
        for ((i, m) in candidates.withIndex()) {
            r -= weights[i]
            if (r <= 0) return remember(c, m)
        }
        return remember(c, candidates.last())
    }

    private fun remember(c: Context, m: Move): Move {
        Prefs.setCurrentMove(c, m.id)
        Prefs.pushRecentMove(c, m.id)
        return m
    }
}
