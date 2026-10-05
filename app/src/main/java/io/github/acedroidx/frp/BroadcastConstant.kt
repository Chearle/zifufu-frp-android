package io.github.acedroidx.frp

object BroadcastAction {
    const val START = "${AppIdentity.EXPECTED_PACKAGE}.START"
    const val STOP = "${AppIdentity.EXPECTED_PACKAGE}.STOP"
}

object BroadcastExtraKey {
    const val TYPE = "TYPE"
    const val NAME = "NAME"
}