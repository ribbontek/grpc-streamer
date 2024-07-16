package com.ribbontek.streamer.util

import io.github.serpro69.kfaker.Faker
import kotlin.random.Random

object FakerUtil {
    private val faker = Faker()

    fun email() = faker.internet.email()

    fun firstName() = faker.name.firstName()

    fun lastName() = faker.name.lastName()

    fun addressLine() = faker.address.buildingNumber() + " " + faker.address.streetName()

    fun state() = faker.address.state()

    fun suburb() = faker.address.city()

    fun postcode() = faker.address.postcode()

    fun country() = faker.address.country()

    fun status() = faker.emotion.noun()

    fun quantity() = faker.random.nextInt(1, 100)

    fun price() = (faker.random.nextInt(100) + faker.random.nextDouble()).toBigDecimal()

    fun password(): String = letter(true) + alphanumeric(8) + letter(false) + numeric() + "$!"

    fun letter(upper: Boolean): Char = faker.random.nextLetter(upper)

    fun alphanumeric(length: Int = 255) = faker.random.randomString(length = length)

    fun code() = faker.random.nextInt(100000..999999)

    fun numeric(): Int = faker.random.nextInt(1, 9)

    inline fun <reified T : Enum<*>> enum(): T = T::class.java.enumConstants[Random.nextInt(0, T::class.java.enumConstants.size)]
}
