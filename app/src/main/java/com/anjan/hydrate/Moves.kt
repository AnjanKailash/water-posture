package com.anjan.hydrate

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
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
    /** Title; "%d" is replaced by the (personalised) rep count. */
    val nameTemplate: String,
    val category: Category,
    val summary: String,
    val steps: List<String>,
    val benefits: List<String>,
    val cautions: List<String> = emptyList(),
    val reps: Int = 0,              // > 0: scaled by age
    val seconds: Int = 0,           // > 0: countdown timer on the how-to page
    val scaleSeconds: Boolean = false,
    val intense: Boolean = false,   // not within 1 hour after a meal
    val seniorCaution: Boolean = false, // not suggested automatically for 60+
    val afterMeal: Boolean = false  // the post-meal walk
) {
    fun title(c: Context): String =
        if (reps > 0) nameTemplate.format(Profile.scaledReps(c, reps)) else nameTemplate

    fun timerSeconds(c: Context): Int =
        if (scaleSeconds) Profile.scaledSeconds(c, seconds) else seconds
}

object Moves {
    private const val MEAL_NOTE = "Not right after breakfast, lunch or dinner — wait about an hour."

    val all: List<Move> = listOf(
        // ---------- Strength ----------
        Move("pushups", "💪", "%d Push-ups", Category.STRENGTH,
            "Floor, knee or desk push-ups — your choice.",
            listOf("Hands slightly wider than shoulders.",
                "Body in a straight line from head to heels (or knees).",
                "Lower your chest, then push back up.",
                "Too hard? Push against your desk or a wall."),
            benefits = listOf("Strengthens chest, shoulders, arms and core.",
                "Gets your heart rate up in under a minute.",
                "Counters the rounded-shoulder desk posture."),
            cautions = listOf("Skip with shoulder or wrist injuries — try desk push-ups instead.", MEAL_NOTE),
            reps = 10, intense = true),
        Move("squats", "🦵", "%d Squats", Category.STRENGTH,
            "Sit back like into a chair, then stand tall.",
            listOf("Feet shoulder-width apart, arms forward.",
                "Push hips back and bend knees as if sitting on a chair.",
                "Keep chest up and heels down, then stand tall."),
            benefits = listOf("Works the biggest muscles in your body (legs and glutes).",
                "Boosts blood flow after long sitting.",
                "Helps keep everyday movements like standing up easy as you age."),
            cautions = listOf("Go shallow or hold a chair if your knees hurt.", MEAL_NOTE),
            reps = 15, intense = true),
        Move("situps", "🔥", "%d Crunches", Category.STRENGTH,
            "Quick core work on a mat or carpet.",
            listOf("Lie on your back, knees bent, feet flat.",
                "Hands lightly behind your head — don't pull your neck.",
                "Lift shoulders off the floor, pause, lower slowly."),
            benefits = listOf("Strengthens the abdominal muscles.",
                "A strong core supports better sitting posture."),
            cautions = listOf("Not recommended with neck or lower-back problems.",
                "Avoid during pregnancy.", MEAL_NOTE),
            reps = 15, intense = true, seniorCaution = true),
        Move("plank", "🧱", "Plank hold", Category.STRENGTH,
            "Hold a straight line on your forearms.",
            listOf("Forearms on the floor, elbows under shoulders.",
                "Straighten legs, body in one line, squeeze your stomach.",
                "Breathe normally and hold until the timer ends.",
                "Easier: knees down."),
            benefits = listOf("Builds deep core strength that protects your back.",
                "Improves posture and balance.",
                "No equipment and very little space needed."),
            cautions = listOf("Not recommended with high blood pressure — don't hold your breath.",
                "Avoid with shoulder injuries or during pregnancy.", MEAL_NOTE),
            seconds = 30, scaleSeconds = true, intense = true, seniorCaution = true),
        Move("wallsit", "🪑", "Wall sit", Category.STRENGTH,
            "The 'invisible chair' against a wall.",
            listOf("Back flat against a wall.",
                "Slide down until knees are bent (aim for 90°, higher is fine).",
                "Knees over ankles, arms relaxed.",
                "Hold until the timer ends, then stand up slowly."),
            benefits = listOf("Strengthens thighs and glutes with zero impact.",
                "Builds endurance for stairs and walking.",
                "Easy to do in an office corner."),
            cautions = listOf("Not recommended with knee pain — stay higher up the wall or skip.",
                "Avoid with high blood pressure if it makes you strain.", MEAL_NOTE),
            seconds = 30, scaleSeconds = true, intense = true),
        Move("lunges", "🏃", "%d Lunges each leg", Category.STRENGTH,
            "Step forward, lower, push back.",
            listOf("Stand tall, step one foot forward.",
                "Lower until both knees are about 90°.",
                "Push through the front heel to come back. Alternate legs."),
            benefits = listOf("Strengthens legs one side at a time.",
                "Improves balance and hip mobility."),
            cautions = listOf("Not recommended with knee or balance problems — hold a wall or skip.", MEAL_NOTE),
            reps = 10, intense = true, seniorCaution = true),
        Move("calves", "🦶", "%d Calf raises", Category.STRENGTH,
            "Easy to do right at your desk.",
            listOf("Stand holding your desk or chair for balance.",
                "Rise up onto your toes, pause one second.",
                "Lower slowly."),
            benefits = listOf("Pumps blood back up from your legs after sitting.",
                "Strengthens ankles and calves.",
                "Gentle enough for almost anyone."),
            cautions = listOf("Hold on to something steady if your balance isn't great."),
            reps = 20),
        Move("deskpush", "🖥️", "%d Desk push-ups", Category.STRENGTH,
            "Office-friendly push-ups against your desk.",
            listOf("Hands on the edge of a sturdy desk, shoulder-width.",
                "Walk feet back until your body is in a straight line.",
                "Bend elbows to bring chest to the desk, push back."),
            benefits = listOf("Upper-body strength without getting on the floor.",
                "Wakes you up without breaking a sweat."),
            cautions = listOf("Make sure the desk won't slide or tip.", "Skip with wrist pain."),
            reps = 10),

        // ---------- Stretch ----------
        Move("neck", "🙆", "Neck rolls", Category.STRETCH,
            "Release tension from screen time.",
            listOf("Sit tall, shoulders relaxed.",
                "Slowly roll your head in a circle, 5 times one way.",
                "Then 5 times the other way. Move gently."),
            benefits = listOf("Eases neck stiffness from looking at screens.",
                "Can help reduce tension headaches."),
            cautions = listOf("Move slowly. Stop if you feel dizziness or pain.",
                "Skip with a neck injury — gentle side tilts are safer.")),
        Move("shoulders", "🤷", "Shoulder rolls", Category.STRETCH,
            "Undo the hunched-over-laptop posture.",
            listOf("Arms hanging loose.",
                "Roll shoulders up, back and down — 10 times.",
                "Then 10 times forward.",
                "Finish by squeezing shoulder blades together for 5 seconds."),
            benefits = listOf("Loosens tight shoulders and upper back.",
                "Resets posture after typing.")),
        Move("chest", "🚪", "Doorway chest stretch", Category.STRETCH,
            "Open up the chest after sitting.",
            listOf("Stand in a doorway, forearms on the frame at shoulder height.",
                "Step one foot forward until you feel a stretch across your chest.",
                "Hold and breathe slowly."),
            benefits = listOf("Opens the chest muscles that tighten when you hunch.",
                "Makes it easier to sit up straight."),
            cautions = listOf("Gentle stretch only — avoid with shoulder dislocation history."),
            seconds = 30),
        Move("hamstring", "🦿", "Hamstring stretch", Category.STRETCH,
            "Loosen the backs of your legs.",
            listOf("Put one heel on a low step or chair, leg straight.",
                "Keep your back straight and lean forward from the hips.",
                "Hold 15 seconds, then switch legs."),
            benefits = listOf("Relieves tight hamstrings from sitting.",
                "Tight hamstrings can pull on the lower back — this helps."),
            cautions = listOf("Don't bounce. Ease off if you feel pain behind the knee."),
            seconds = 30),
        Move("wrists", "✋", "Wrist & finger stretch", Category.STRETCH,
            "Great after lots of typing.",
            listOf("Arm straight in front, palm up.",
                "With the other hand, gently pull fingers back — 10 seconds.",
                "Turn palm down and press the back of the hand — 10 seconds.",
                "Switch hands. Finish by spreading fingers wide 5 times."),
            benefits = listOf("Relieves strain from typing and phone use.",
                "Helps keep wrists flexible.")),
        Move("twist", "🌀", "Seated spinal twist", Category.STRETCH,
            "Gentle twist without leaving your chair.",
            listOf("Sit tall, feet flat.",
                "Turn your upper body to the right, hand on the chair back.",
                "Hold 15 seconds, breathe, then switch sides."),
            benefits = listOf("Mobilises the spine and eases lower-back stiffness.",
                "A calm, quiet reset you can do in a meeting room."),
            cautions = listOf("Twist gently. Avoid with a recent back injury or disc problems.",
                "Better before a meal than straight after one."),
            seconds = 30),

        // ---------- Mind & coordination ----------
        Move("fingertap", "🖐️", "Finger tapping race", Category.MIND,
            "Thumb touches each finger — then mix up the hands.",
            listOf("Touch your thumb to each finger: index → middle → ring → little, and back.",
                "Do it with both hands together, 3 times.",
                "Now the tricky part: left hand goes forward while right hand goes backward.",
                "Speed up as you get better!"),
            benefits = listOf("Challenges coordination between both sides of the brain.",
                "A quick mental refresh between tasks.",
                "Keeps fingers nimble.")),
        Move("thumbpinky", "👍", "Thumb–pinky switch", Category.MIND,
            "A classic brain-coordination drill.",
            listOf("Make two fists in front of you.",
                "Left hand: thumb up. Right hand: little finger out.",
                "Switch at the same time: left little finger, right thumb.",
                "Keep switching 10 times, as fast as you can."),
            benefits = listOf("Trains focus and hand coordination.",
                "Funny enough to lift your mood.")),
        Move("circlesquare", "🔵", "Circle & square", Category.MIND,
            "Draw two different shapes at once.",
            listOf("Right hand draws circles in the air.",
                "At the same time, left hand draws squares.",
                "Keep going until the timer ends, then swap hands."),
            benefits = listOf("Makes both hands work independently — great brain exercise.",
                "Breaks the autopilot of screen work."),
            seconds = 20),
        Move("noseear", "👃", "Nose & ear switch", Category.MIND,
            "Cross-body coordination with a clap.",
            listOf("Left hand touches your nose, right hand holds your left ear.",
                "Clap once.",
                "Switch: right hand on nose, left hand on right ear.",
                "Repeat 10 times, getting faster."),
            benefits = listOf("Cross-body movement that sharpens attention.",
                "Gets you laughing — a quick stress release.")),
        Move("countback", "🔢", "Count back by 7", Category.MIND,
            "A 1-minute focus reset.",
            listOf("Start at 100.",
                "Count down by 7: 93, 86, 79 …",
                "Go as far as you can before the timer ends."),
            benefits = listOf("Exercises working memory and concentration.",
                "Clears your head before the next task."),
            seconds = 60),

        // ---------- Eyes ----------
        Move("eyes", "👀", "20-20-20 eye break", Category.EYES,
            "Rest your eyes from the screen.",
            listOf("Look at something about 20 feet (6 m) away.",
                "Keep looking until the timer ends.",
                "Blink slowly a few times before going back to the screen."),
            benefits = listOf("Relaxes the eye muscles that focus up close.",
                "Can reduce eye strain and dryness from screens."),
            seconds = 20),

        // ---------- Breathing ----------
        Move("boxbreath", "🌬️", "Box breathing", Category.BREATH,
            "Calm down in about a minute.",
            listOf("Breathe in for 4 seconds.",
                "Hold for 4 seconds.",
                "Breathe out for 4 seconds.",
                "Hold for 4 seconds. Repeat until the timer ends."),
            benefits = listOf("Slows your breathing and helps you feel calmer.",
                "Useful before a meeting or after a stressful call."),
            cautions = listOf("If holding your breath feels uncomfortable, just breathe in and out slowly."),
            seconds = 64),

        // ---------- Walk ----------
        Move("walk2", "🚶", "2-minute walk", Category.WALK,
            "Walk around — refill your water bottle on the way.",
            listOf("Stand up and walk around the office or home.",
                "Take the stairs if you can.",
                "Refill your water bottle while you're up."),
            benefits = listOf("Breaks up long sitting, which matters even if you exercise later.",
                "Gets blood flowing to your legs and brain."),
            seconds = 120),
        Move("mealwalk", "🌳", "Walk after your meal (15–30 min)", Category.WALK,
            "An easy walk after eating helps digestion and energy.",
            listOf("Head out for an easy walk — 15 minutes minimum, 30 if you can.",
                "Comfortable pace; you should still be able to talk.",
                "Outdoor light is a bonus for energy."),
            benefits = listOf("A gentle walk after eating can help keep blood sugar steadier.",
                "Helps digestion and beats the post-meal slump.",
                "Adds easy steps to your day."),
            cautions = listOf("Keep it easy — no running straight after a meal."),
            afterMeal = true)
    )

    fun byId(id: String?): Move? = all.firstOrNull { it.id == id }
}

/** Optional personal details, stored only on the phone. */
object Profile {
    /** Rep / hold-time multiplier by age. */
    private fun factor(c: Context): Double = when (val age = Prefs.age(c)) {
        0 -> 1.0
        in 1..39 -> 1.0
        in 40..59 -> 0.8
        else -> if (age >= 60) 0.6 else 1.0
    }

    fun scaledReps(c: Context, base: Int) = max(3, (base * factor(c)).roundToInt())
    fun scaledSeconds(c: Context, base: Int) = max(15, ((base * factor(c)) / 5).roundToInt() * 5)

    fun isSenior(c: Context) = Prefs.age(c) >= 60
    fun hasProfile(c: Context) = Prefs.age(c) > 0 || Prefs.weightKg(c) > 0 || Prefs.sex(c) != Prefs.SEX_UNSET

    /** Rough daily drinking-water guide in ml. General guidance, not medical advice. */
    fun dailyWaterMl(c: Context): Int {
        val w = Prefs.weightKg(c)
        val ml = if (w > 0) w * 33 else when (Prefs.sex(c)) {
            Prefs.SEX_MALE -> 2500
            Prefs.SEX_FEMALE -> 2000
            else -> 2200
        }
        return ml.coerceIn(1500, 4000)
    }

    /** Reminders per day in the user's start–end window. */
    fun remindersPerDay(c: Context): Int {
        var span = (Prefs.endMinutes(c) - Prefs.startMinutes(c) + 1440) % 1440
        if (span == 0) span = 1440
        return span / Prefs.intervalMinutes(c) + 1
    }

    /** Suggested sip per reminder, rounded to 50 ml. */
    fun mlPerReminder(c: Context): Int {
        val raw = dailyWaterMl(c).toDouble() / remindersPerDay(c)
        return ((raw / 50).roundToInt() * 50).coerceIn(100, 400)
    }
}

/**
 * Chooses the activity for each reminder.
 * Smart picks: whole library, shaped by time of day, meals, age and variety.
 * My picks: only the activities the user ticked, still varied and meal-aware.
 */
object Recommender {

    private fun dayKey(t: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(t))

    fun current(c: Context): Move? = Moves.byId(Prefs.currentMove(c))

    /** Minutes since the most recent meal today (negative/huge if none yet). */
    private fun minutesSinceMeal(c: Context, minuteOfDay: Int): Int {
        val meals = listOf(Prefs.breakfastMinutes(c), Prefs.lunchMinutes(c), Prefs.dinnerMinutes(c))
        return meals.map { minuteOfDay - it }.filter { it >= 0 }.minOrNull() ?: 9999
    }

    fun pickNext(c: Context, now: Long = System.currentTimeMillis()): Move {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val sinceMeal = minutesSinceMeal(c, minuteOfDay)
        val smart = Prefs.moveMode(c) == Prefs.MOVE_SMART
        val senior = Profile.isSenior(c)

        var pool = if (smart) Moves.all else Moves.all.filter { it.id in Prefs.selectedMoves(c) }
        if (smart && senior) pool = pool.filter { !it.seniorCaution }
        if (pool.isEmpty()) pool = Moves.all.filter { !it.intense }

        // 1) First reminder within 90 min after lunch or dinner: suggest a walk (once per meal per day).
        val today = dayKey(now)
        val walk = pool.firstOrNull { it.afterMeal }
        if (walk != null) {
            for ((key, meal) in listOf("lunch" to Prefs.lunchMinutes(c), "dinner" to Prefs.dinnerMinutes(c))) {
                val since = minuteOfDay - meal
                if (since in 0..90 && Prefs.mealWalkDay(c, key) != today) {
                    Prefs.setMealWalkDay(c, key, today)
                    return remember(c, walk)
                }
            }
        }

        // 2) Otherwise a weighted random pick.
        var candidates = pool.filter { !it.afterMeal }
        if (sinceMeal in 0..59) {
            // Within an hour after a meal: no intense moves if anything gentler is available.
            val gentle = candidates.filter { !it.intense }
            if (gentle.isNotEmpty()) candidates = gentle
        }
        if (candidates.isEmpty()) candidates = pool

        val recent = Prefs.recentMoves(c)
        val lastCategory = Moves.byId(recent.lastOrNull())?.category

        fun weight(m: Move): Double {
            var w = 1.0
            if (m.id in recent) w *= 0.15                     // not again so soon
            if (m.category == lastCategory) w *= 0.4          // switch it up
            if (!smart && senior && m.seniorCaution) w *= 0.3
            if (smart) {
                if (minuteOfDay < 11 * 60 && m.category == Category.STRETCH) w *= 1.8  // morning mobility
                if (minuteOfDay in (15 * 60)..(17 * 60) &&
                    (m.category == Category.STRENGTH || m.category == Category.WALK)) w *= 1.6  // afternoon slump
                if (minuteOfDay >= 19 * 60 && m.intense) w *= 0.5                 // calmer evenings
                if (senior && (m.category == Category.STRETCH || m.category == Category.MIND)) w *= 1.4
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
