package com.github.vonnagy.service.container.http

/**
 * Generic response to Marshal for sending rejection information back to the client.
 * @param code the code for the rejection.
 * @param message the message of the rejection
 * @param details the details of the rejection
 */
case class RejectionResponse(code: Int, message: String, details: String)
