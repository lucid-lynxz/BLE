package org.lynxz.ble_lib.util

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import java.util.concurrent.atomic.AtomicInteger


//fun main(args: Array<String>) = runBlocking<Unit> {
//    val job = launch(CommonPool) { // create new coroutine and keep a reference to its Job
//        delay(1000L)
//        println("World!")
//    }
//    println("Hello,")
//    job.join() // wait until child coroutine completes
//}


//fun main(args: Array<String>) = runBlocking {
//    launch(CommonPool) {
//        repeat(10) { i ->
//            println("I'm sleeping $i ...")
//            delay(500L)
//        }
//    }
//    delay(1300L) // just quit after delay
//}


//fun main(args: Array<String>) = runBlocking {
//    val job = launch(CommonPool) {
//        repeat(1000) { i ->
//            println("I'm sleeping $i ...")
//            delay(500L)
//        }
//    }
//    delay(1300L) // delay a bit
//    println("main: I'm tired of waiting!")
//    job.cancel() // cancels the job
//    delay(1300L) // delay a bit to ensure it was cancelled indeed
//    println("main: Now I can quit.")
//}

//fun main(args: Array<String>) = runBlocking<Unit> {
//    val job = launch(CommonPool) {
//        var nextPrintTime = 0L
//        var i = 0
//        while (i < 10) { // computation loop
//            val currentTime = System.currentTimeMillis()
//            if (currentTime >= nextPrintTime) {
//                println("I'm sleeping ${i++} ...")
//                nextPrintTime = currentTime + 200L
//            }
//        }
//    }
//    delay(1300L) // delay a bit
//    println("main: I'm tired of waiting!")
//    job.cancel() // cancels the job
//    println("main: job canceling ...")
//    delay(1300L) // delay a bit to see if it was cancelled....
//    println("main: Now I can quit.")
//}


//fun main(args: Array<String>) = runBlocking<Unit> {
//    val job = launch(CommonPool) {
//        var nextPrintTime = 0L
//        var i = 0
//        while (isActive) { // cancellable computation loop
//            val currentTime = System.currentTimeMillis()
//            if (currentTime >= nextPrintTime) {
//                println("I'm sleeping ${i++} ...")
//                nextPrintTime = currentTime + 200L
//            }
//        }
//    }
//    delay(1300L) // delay a bit
//    println("main: I'm tired of waiting!")
//    job.cancel() // cancels the job
//    delay(1300L) // delay a bit to see if it was cancelled....
//    println("main: Now I can quit.")
//}

//fun main(args: Array<String>) = runBlocking<Unit> {
//    val job = launch(CommonPool) {
//        try {
//            repeat(1000) { i ->
//                println("I'm sleeping $i ...")
//                delay(500L)
//            }
//        } finally {
//            println("I'm running finally")
//        }
//    }
//    delay(1300L) // delay a bit
//    println("main: I'm tired of waiting!")
//    job.cancel() // cancels the job
//    delay(1300L) // delay a bit to ensure it was cancelled indeed
//    println("main: Now I can quit.")
//}


//fun main(args: Array<String>) = runBlocking<Unit> {
//    val job = launch(CommonPool) {
//        try {
//            repeat(1000) { i ->
//                println("I'm sleeping $i ...")
//                delay(500L)
//            }
//        } finally {
//            run(NonCancellable) {
//                println("I'm running finally")
//                delay(1000L)
//                println("And I've just delayed for 1 sec because I'm non-cancellable")
//            }
//        }
//    }
//    delay(1300L) // delay a bit
//    println("main: I'm tired of waiting!")
//    job.cancel() // cancels the job
//    delay(1300L) // delay a bit to ensure it was cancelled indeed
//    println("main: Now I can quit.")
//}

fun main(args: Array<String>) {
    val c = AtomicInteger()
    for (i in 1..1_000_000)
        launch(CommonPool) {
            c.addAndGet(i)
        }
//        thread(start = true) {
//            c.addAndGet(i)
//        }

    println(c.get())

    val deferred = (1..100).map { n ->
        async(CommonPool) {
            n
        }
    }

    runBlocking {
        val sum = deferred.sumBy { it.await() }
        println("sum $sum")
    }
}

