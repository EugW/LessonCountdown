package pro.eugw.lessoncountdown.util

class StatusController {

    var serviceStatus = false
    var workerStatus = false

    companion object {
        var inst: StatusController? = null
        fun getInstance(): StatusController {
            if (inst == null)
                inst = StatusController()
            return inst as StatusController
        }
    }

}