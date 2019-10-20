package pro.eugw.lessoncountdown.list.marksLog

import com.google.gson.JsonArray

data class MarksLogElement(val iStart: Int,
                           val iEnd: Int,
                           val timeStart: Long,
                           val timeEnd: Long,
                           val omBefore: Int,
                           val omAfter: Int,
                           val newMarks: JsonArray,
                           val diffSize: Int,
                           val notSent: Int)