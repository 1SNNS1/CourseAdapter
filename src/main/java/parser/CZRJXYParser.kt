package main.java.parser

import main.java.bean.TimeDetail
import main.java.bean.TimeTable
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import parser.Parser
import bean.Course as Course

//广州软件学院
class GZRJXYParser(val username:String,val password:String) : Parser("") {
   override fun generateCourseList(): List<Course> {
        val URL="http://class.seig.edu.cn:7001/sise"
        val UserAgent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36"
        //登录获取cookie
        //登录信息
        val d = Jsoup.connect(URL+"/login.jsp").header("User-Agent",UserAgent).post()
        val elements: List<Element> = d.select("form")
        val datas: MutableMap<String, String> = HashMap()
        for (element in elements[0].getAllElements()) {
            if (element.attr("name").equals("username")) {
                element.attr("value", username)
            }
            if (element.attr("name").equals("password")) {
                element.attr("value", password)
            }
            if (element.attr("name").length > 0) {
                datas[element.attr("name")] = element.attr("value")
            }
        }
        //获取登录cookie
        val connection2 = Jsoup.connect(URL+"/login_check_login.jsp")
            .header("User-Agent", UserAgent)
        val response: Connection.Response =connection2.followRedirects(true).method(Connection.Method.GET)
            .data(datas).execute()
        val map: Map<String, String> = response.cookies()
        val Cookie="JSESSIONID="+map["JSESSIONID"]


        //定义一个存放所有的课程
        val courseList = ArrayList<Course>()
        //截取课程表页面
        val doc = Jsoup.connect(URL+"/module/student_schedular/student_schedular.jsp")
            .followRedirects(false)
            .header("Cookie", Cookie)
            .header("User-Agent", UserAgent)
            .postDataCharset("gbk")
            .post()

        //取相应的所有数据
        val table=doc.select("td[width=10%][align=left][valign=top][class=font12]")
        //存放节数
        val start_node_map:Map<Int,Int> = mapOf(0 to 1 , 1 to 3 , 2 to 5 , 3 to 7 , 4 to 9 , 5 to 11 , 6 to 13 , 7 to 15)
        val end_node_map:Map<Int,Int> = mapOf(0 to 2 , 1 to 4 , 2 to 6 , 3 to 8 , 4 to 10 , 5 to 12 , 6 to 14 , 7 to 16)
        var j:Int=0
        //无脑循环，将每个课整理加入列表
        for(i in table){
            ++j
            var i_html=i.html()
            var i_text=i_html.toString()
            if(i_text=="&nbsp;"){
                continue
            }
            else {
                var name1 = i_text.substringBefore("(")
                i_text=i_text.replace(name1 + "(", "")
                var name2 = i_text.substringBefore(" ")
                var name = name1 + name2
                i_text=i_text.replaceFirst(name2 + " ", "")
                var teacher = i_text.substringBefore(" ")
                i_text=i_text.replaceFirst(teacher + " ", "")
                var workall = i_text.substringBefore("周")
                i_text=i_text.replaceFirst(workall + "周 ", "")
                var room = i_text.substringBefore(")")
                var workday = workall.split(" ")
                var startWeek = workday[0].toInt()
                var endWeek = workday[workday.size - 1].toInt()

                var startNode = start_node_map.get((j-1)/7)!!.toInt()
                var realEndNode = end_node_map.get((j-1)/7)!!.toInt()
                var dayWeek = j%7+1
                var type: Int?
                //判断单双每周
                if (workday.size.toInt() == workday[workday.size - 1].toInt()) {
                    type = 0
                } else {
                    if (workday[0].toInt() % 2 == 0) {
                        type = 2
                    } else {
                        type = 1
                    }
                }
                var course = Course(
                    name = name,
                    room = room,
                    teacher = teacher,
                    day = dayWeek, // dayWeek 代表星期X
                    startNode = startNode,
                    endNode = realEndNode,
                    type = type,
                    startWeek = startWeek, // startWeek-endWeek 代表 x周-y周
                    endWeek = endWeek
                )
                courseList.add(course)
                continue
                    }
                }
       return courseList
    }
    override fun generateTimeTable(): TimeTable?{
        //时间列表
        val TimeList_ = ArrayList<TimeDetail>()
        //存放时间
        val start_time_map:Map<Int,String> = mapOf(1 to "09:00",2 to "09:40",3 to "10:40",4 to "11:20",5 to "12:30",6 to "13:10",7 to "14:00",8 to "14:40",9 to "15:30",10 to "16:10",11 to "17:00",12 to "17:40",13 to "19:00",14 to "19:40",15 to "20:30",16 to "21:10")
        val end_time_map:Map<Int,String> = mapOf(1 to "09:40",2 to "10:20",3 to "11:20",4 to "12:00",5 to "13:10",6 to "13:50",7 to "14:40",8 to "15:20",9 to "16:10",10 to "16:50",11 to "17:40",12 to "18:20",13 to "19:40",14 to "20:20",15 to "21:10",16 to "21:50")
        for(i in 1..16) {
            var timeDetail_ = TimeDetail(
                node = i,
                startTime = start_time_map.get(i).toString(),
                endTime = end_time_map.get(i).toString()
            )
            TimeList_.add(timeDetail_)
        }
        val TimeTable=TimeTable(
            name="gzrjxy",
            timeList = TimeList_
        )
        println(TimeTable)
        return TimeTable
    }

}
