package org.gamekins

import java.io.Serializable

class StoreChallenge: Serializable {

    lateinit var job: String;
    lateinit var challengeName: String;
    lateinit var reason: String;

    constructor()
    constructor(job: String,challengeName: String, reason: String ) {
        this.job = job
        this.challengeName = challengeName
        this.reason = reason
    }
}
