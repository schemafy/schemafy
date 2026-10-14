package com.schemafy.api.erd.service.sync;

/** Thrown when a Lua-scripted state transition (complete/requeue) is rejected
 * because the caller's lease token, generation, or kind no longer matches
 * the job's current state in Redis. This always means a concurrent enqueue
 * call already superseded and rescheduled the job. */
final class JobTransitionRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  JobTransitionRejectedException(String message) {
    super(message);
  }

}
