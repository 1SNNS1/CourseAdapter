package parser

import Common
import bean.Course
import main.java.bean.TimeDetail
import main.java.bean.TimeTable

class THUParser(source: String) : Parser(source) {

    // 固定数据

    val startNodeMap = mapOf(1 to 1, 2 to 3, 3 to 6, 4 to 8, 5 to 10, 6 to 12)
    val endNodeMap = mapOf(1 to 2, 2 to 5, 3 to 7, 4 to 9, 5 to 11, 6 to 14)

    override fun generateTimeTable(): TimeTable {
        return TimeTable(
            name = "清华大学", timeList = listOf(
                TimeDetail(1, "08:00", "08:45"),
                TimeDetail(2, "08:50", "09:35"),
                TimeDetail(3, "09:50", "10:35"),
                TimeDetail(4, "10:40", "11:25"),
                TimeDetail(5, "11:30", "12:15"),
                TimeDetail(6, "13:30", "14:15"),
                TimeDetail(7, "14:20", "15:05"),
                TimeDetail(8, "15:20", "16:05"),
                TimeDetail(9, "16:10", "16:55"),
                TimeDetail(10, "17:05", "17:50"),
                TimeDetail(11, "17:55", "18:40"),
                TimeDetail(12, "19:20", "20:05"),
                TimeDetail(13, "20:10", "20:55"),
                TimeDetail(14, "21:00", "21:45")
            )
        )
    }

    // 学期数据

    lateinit var semester: String

    fun getRescheduleData(): List<Reschedule> {
        return when (semester) {
            "2020-2021-1" -> listOf(
                Reschedule(fromWeek = 3, fromDay = 5, toWeek = 2, toDay = 7),
                Reschedule(fromWeek = 3, fromDay = 6),
                Reschedule(fromWeek = 3, fromDay = 7),
                Reschedule(fromWeek = 3, fromDay = 4, toWeek = 4, toDay = 6)
            )
            "2020-2021-2" -> listOf(
                Reschedule(fromWeek = 7, fromDay = 1),
                Reschedule(fromWeek = 9, fromDay = 6),
                Reschedule(fromWeek = 9, fromDay = 7),
                Reschedule(fromWeek = 10, fromDay = 5),
                Reschedule(fromWeek = 10, fromDay = 6, toWeek = 11, toDay = 2),
                Reschedule(fromWeek = 10, fromDay = 7, toWeek = 11, toDay = 3),
                Reschedule(fromWeek = 11, fromDay = 1, toWeek = 11, toDay = 4),
                Reschedule(fromWeek = 16, fromDay = 6),
                Reschedule(fromWeek = 16, fromDay = 7)
            )
            "2021-2022-1" -> listOf(
                Reschedule(fromWeek = 2, fromDay = 1, toWeek = 1, toDay = 6),
                Reschedule(fromWeek = 2, fromDay = 2, toWeek = 2, toDay = 7),
                Reschedule(fromWeek = 3, fromDay = 5, toWeek = 4, toDay = 6),
                Reschedule(fromWeek = 3, fromDay = 6),
                Reschedule(fromWeek = 3, fromDay = 7)
            )
            "2021-2022-2" -> listOf(
                Reschedule(fromWeek = 7, fromDay = 1, toWeek = 6, toDay = 6),
                Reschedule(fromWeek = 7, fromDay = 2),
                Reschedule(fromWeek = 9, fromDay = 6),
                Reschedule(fromWeek = 9, fromDay = 7),
                Reschedule(fromWeek = 10, fromDay = 7),
                Reschedule(fromWeek = 11, fromDay = 1),
                Reschedule(fromWeek = 11, fromDay = 2, toWeek = 11, toDay = 6),
                Reschedule(fromWeek = 11, fromDay = 3),
                Reschedule(fromWeek = 11, fromDay = 4),
                Reschedule(fromWeek = 15, fromDay = 5),
                Reschedule(fromWeek = 16, fromDay = 6),
                Reschedule(fromWeek = 16, fromDay = 7)
            )
            else -> listOf()
        }
    }

    fun getTotalWeeks(): Int {
        return when (semester) {
            "2021-2022-1" -> 15
            else -> 16
        }
    }

    val supportedPKUSemesters = listOf("2020-2021-2", "2021-2022-1", "2021-2022-2")

    fun getPKUTotalWeeks(): Int {
        return when (semester) {
            "2020-2021-2" -> 15
            else -> 16
        }
    }

    fun getPKUWeekShift(): Int {
        return when (semester) {
            "2020-2021-1" -> 1
            "2020-2021-2" -> 2
            else -> 0
        }
    }

    fun getPKUCourseNotesSeparator(): String {
        return when (semester) {
            "2020-2021-2" -> ";"
            "2021-2022-1" -> ","
            "2021-2022-2" -> "；"
            else -> ","
        }
    }

    fun getPKUHolidayWeeks(day: Int): List<Int> {
        return when (semester) {
            "2020-2021-2" -> when (day) {
                5, 6 -> listOf(8)
                else -> listOf()
            }
            "2021-2022-1" -> when (day) {
                1, 3, 4 -> listOf(4)
                2 -> listOf(2, 4)
                5, 6, 7 -> listOf(3)
                else -> listOf()
            }
            "2021-2022-2" -> when (day) {
                // 清明和端午安排未出
                1, 2, 3, 4, 5 -> listOf(11)
                7 -> listOf(10)
                else -> listOf()
            }
            else -> listOf()
        }
    }

    // 课程表解析

    lateinit var courseList: MutableList<Course>
    lateinit var secondaryCoursesDetails: Map<String, CourseDetails>  // contains teacher and notes
    lateinit var rescheduleList: List<Reschedule>

    val semesterRegex = Regex("""name="p_xnxq" value="([\d\-]+?)"""")

    override fun generateCourseList(): List<Course> {
        semester = semesterRegex.find(source)?.groupValues?.get(1) ?: ""
        courseList = mutableListOf()
        rescheduleList = getRescheduleData()

        parseSecondaryCourseTable()  // generates secondaryCoursesDetails and parses PKU courses
        parseCourses()

        return courseList
    }

    val mainScriptRegex = Regex("""setInitValue\(\).+setInitValue""", RegexOption.DOT_MATCHES_ALL)
    val cellPositionRegex = Regex("""a(\d)_(\d)""")
    val blueTextRegex = Regex("""<font color='blue'>([^<>]+?)</font>""")
    val courseNumberRegex = Regex("""\d{10};(\d{8})""")  // teacher ID; course number

    fun parseCourses() {
        val totalWeeks = getTotalWeeks()
        val script = mainScriptRegex.find(source)!!.value
        var courseInfo = CourseDetails()
        script.lines().forEach { line ->
            if ("strHTML += \"" in line) {
                if ("<a " in line) {
                    courseInfo.number = courseNumberRegex.find(line)?.groupValues?.get(1) ?: ""
                } else if ("<b>" in line) {
                    courseInfo.name = line.substringAfter("<b>").substringBefore("</b>").trim()
                }
            } else if ("strHTML1 +=" in line) {
                courseInfo.params.add(line.substringAfter("；").substringBefore("\"").trim())
            } else if ("blue_red_none" in line) {
                // secondary courses
                var topic = ""
                blueTextRegex.findAll(line).forEachIndexed { i, result ->
                    when (i) {
                        0 -> courseInfo.name = result.groupValues[1].trim()
                        1 -> {
                            var details = result.groupValues[1].trim()
                            if (details.startsWith("(") && details.count { it == '(' } >= 2) {
                                topic = details.substringBefore(")") + ")"
                                details = details.substringAfter(")")
                            }
                            details = details.substringAfter("(").substringBefore(")").trim()
                            details.split("；").forEach {
                                when {
                                    "周" in it -> courseInfo.weeks = it
                                    "时间：" in it -> courseInfo.time = it.removePrefix("时间：")
                                    courseInfo.location.isBlank() -> courseInfo.location = it.trim()
                                }
                            }
                        }
                    }
                }
                courseInfo.location += topic
                courseInfo.teacher = secondaryCoursesDetails[courseInfo.name]?.teacher ?: ""
                courseInfo.notes = secondaryCoursesDetails[courseInfo.name]?.notes ?: ""
            } else if ("getElementById" in line) {
                // finalize
                val (_, node, day) = cellPositionRegex.find(line)!!.groupValues
                if (courseInfo.params.isNotEmpty()) {
                    courseInfo.params.reverse()
                    courseInfo.params.forEach {
                        when {
                            // 倒数第一个以周结尾的是周数
                            it.endsWith("周") && courseInfo.weeks.isBlank() -> courseInfo.weeks = it
                            // 上课教室在周数的后面
                            courseInfo.weeks.isBlank() -> courseInfo.location = it
                            // 周数前面的不是课程属性的是教师（有可能空）
                            it !in Common.courseProperty -> courseInfo.teacher = it
                        }
                    }
                }
                val course = Course(
                    name = courseInfo.name,
                    day = day.toInt(),
                    room = courseInfo.location,
                    teacher = courseInfo.teacher,
                    startNode = startNodeMap.getOrDefault(node.toInt(), 0),
                    endNode = endNodeMap.getOrDefault(node.toInt(), 0),
                    startWeek = 0,
                    endWeek = 0,
                    type = -1,
                    credit = when (courseInfo.number) {
                        "" -> 0.0f
                        else -> courseInfo.number.last().toFloat() - 48 // char to float: ASCII
                    },
                    note = courseInfo.notes,
                    startTime = when (courseInfo.time) {
                        "" -> ""
                        else -> courseInfo.time.substringBefore("-").timeZeroPad()
                    },
                    endTime = when (courseInfo.time) {
                        "" -> ""
                        else -> courseInfo.time.substringAfter("-").timeZeroPad()
                    }
                )
                val weekIntList = parseWeeks(courseInfo.weeks.trim(), totalWeeks)
                rescheduleList.forEach {
                    if (course.day == it.toDay && it.toWeek in weekIntList) {
                        weekIntList.remove(it.toWeek)
                    }
                    if (course.day == it.fromDay && it.fromWeek in weekIntList) {
                        weekIntList.remove(it.fromWeek)
                        if (it.toWeek > 0) {
                            if (course.day == it.toDay && it.toWeek !in weekIntList) {
                                // 目标日的课程已被清除
                                weekIntList.add(it.toWeek)
                            } else {
                                courseList.add(
                                    course.copy(
                                        day = it.toDay,
                                        startWeek = it.toWeek,
                                        endWeek = it.toWeek,
                                        type = 0
                                    )
                                )
                            }
                        }
                    }
                }
                weekIntList.sort()
                Common.weekIntList2WeekBeanList(weekIntList).forEach { week ->
                    courseList.add(
                        course.copy(
                            startWeek = week.start,
                            endWeek = week.end,
                            type = week.type
                        )
                    )
                }
                courseInfo = CourseDetails()  // reset
            }
        }
    }

    val secondaryCourseTableHeaderRegex = Regex("""var gridColumns = \[(.+)];""", RegexOption.DOT_MATCHES_ALL)
    val secondaryCourseTableDataRegex = Regex("""var gridData = \[(.+)];""", RegexOption.DOT_MATCHES_ALL)
    val bracketsRegex = Regex("""\[([^\[\]]+)]""")

    fun parseSecondaryCourseTable() {
        val result = mutableMapOf<String, CourseDetails>()
        val header = secondaryCourseTableHeaderRegex.find(source)?.groupValues?.get(1)
        val data = secondaryCourseTableDataRegex.find(source)?.groupValues?.get(1)
        if (header != null && data != null) {
            var nameIndex = 0
            var weeksIndex = 0
            var locationIndex = 0
            var teacherIndex = 0
            var notesIndex = 0
            header.split(",").forEachIndexed { i, s ->
                when {
                    "课程名" in s -> nameIndex = i
                    "上课周次" in s -> weeksIndex = i
                    "上课地点" in s -> locationIndex = i
                    "任课教师" in s -> teacherIndex = i
                    "选课文字说明" in s -> notesIndex = i
                }
            }
            bracketsRegex.findAll(data).forEach { array ->
                val items = array.groupValues[1].split(",")
                val name = items[nameIndex].trim().removeSurrounding("\"")
                val teacher = items[teacherIndex].trim().removeSurrounding("\"")
                val notes = items[notesIndex].trim().removeSurrounding("\"")
                if ("北大" !in array.groupValues[1]) {
                    result[name] = CourseDetails(teacher = teacher, notes = notes)
                } else if (semester in supportedPKUSemesters) {
                    parsePKUCourse(
                        CourseDetails(
                            name = name,
                            weeks = items[weeksIndex].trim().removeSurrounding("\""),
                            location = items[locationIndex].trim().removeSurrounding("\""),
                            teacher = teacher,
                            notes = notes
                        )
                    )
                }
            }
        }
        secondaryCoursesDetails = result
    }

    fun parsePKUCourse(details: CourseDetails) {
        val teacher = Regex("""教师:(.+?);""").find(details.notes)?.groupValues?.get(1) ?: details.teacher
        val weekShift = getPKUWeekShift()
        val fallbackWeeks = parseWeeks(
            courseWeeks = details.weeks,
            totalWeeks = getPKUTotalWeeks()
        )
        Regex("""时间:(.+)""").find(details.notes)!!.groupValues[1]
            .split(getPKUCourseNotesSeparator()).forEach { time ->
                if (":" !in time) {  // notes
                    return@forEach
                }
                val lastSegment = time.trim().substringAfterLast(' ')
                val weeks = when {
                    "(" in time -> parseWeeks(
                        courseWeeks = time.substringAfter("(").substringBefore(")"),
                        totalWeeks = fallbackWeeks.last()
                    )
                    lastSegment.endsWith("周") -> parseWeeks(
                        courseWeeks = lastSegment,
                        totalWeeks = fallbackWeeks.last()
                    )
                    else -> fallbackWeeks
                }
                val (_, dayStr, startTime, endTime) = Regex("""周(\S)\s*(\d{1,2}:\d{2})-(\d{1,2}:\d{2})""")
                    .find(time)!!.groupValues
                val day = Common.getNodeInt(dayStr)
                getPKUHolidayWeeks(day).forEach { week -> weeks.remove(week); }
                Common.weekIntList2WeekBeanList(weeks.map { it + weekShift }.toMutableList()).forEach { week ->
                    courseList.add(
                        Course(
                            name = details.name,
                            day = day,
                            room = details.location,
                            teacher = teacher,
                            startNode = 0,
                            endNode = 0,
                            startWeek = week.start,
                            endWeek = week.end,
                            type = week.type,
                            note = details.notes,
                            startTime = startTime.timeZeroPad(),
                            endTime = endTime.timeZeroPad()
                        )
                    )
                }
            }
    }

    fun parseWeeks(courseWeeks: String, totalWeeks: Int): MutableList<Int> {
        return when (courseWeeks) {
            "全周" -> 1..totalWeeks
            "前八周" -> 1..8
            "后八周" -> 9..totalWeeks
            "单周" -> 1..totalWeeks step 2
            "双周" -> 2..totalWeeks step 2
            else -> {
                if (!courseWeeks.endsWith("周")) {
                    return mutableListOf()
                }
                val courseWeeksRanges = courseWeeks
                    .removePrefix("第")
                    .removeSuffix("周")
                    .split(",")
                val weekIntList = mutableListOf<Int>()
                courseWeeksRanges.forEach {
                    when {
                        "-" in it ->
                            (it.substringBefore("-").toInt()..it.substringAfter("-").toInt())
                                .forEach { weekInt -> weekIntList.add(weekInt) }
                        else -> weekIntList.add(it.toInt())
                    }
                }
                weekIntList
            }
        }.toMutableList()
    }

    data class Reschedule(
        val fromWeek: Int,
        val fromDay: Int,
        val toWeek: Int = 0,
        val toDay: Int = 0
    )

    data class CourseDetails(
        var number: String = "",
        var name: String = "",
        var teacher: String = "",
        var weeks: String = "",
        var location: String = "", // 二级课程内容加在这里
        var notes: String = "",
        var time: String = "",
        val params: MutableList<String> = mutableListOf()
    )

    fun String.timeZeroPad(): String {
        return if (this.length == 4 && this[0].isDigit() && this[1] == ':' && this[2].isDigit() && this[3].isDigit()) {
            "0$this"
        } else {
            this
        }
    }
}
