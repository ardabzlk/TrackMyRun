package com.example.trackmyrun.model


import java.sql.Date
import java.util.*

data class Post(
    val email: String,
    val comment: String,
    val downloadUrl: String,
    val routeDuration: String,
    val routeDistance: String,
    val startLocation: String,
    val endLocation: String,
    val date: java.util.Date,
)