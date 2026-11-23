package ru.vlyashuk.roadbuddy.domain.model

enum class RequestActor {
    AUTHOR,
    HELPER,
    OTHER
}

object RequestStatusTransitions {

    private val allowedActors: Map<Pair<RequestStatus, RequestStatus>, Set<RequestActor>> = mapOf(
        (RequestStatus.OPEN to RequestStatus.IN_PROGRESS) to setOf(RequestActor.OTHER),
        (RequestStatus.OPEN to RequestStatus.CANCELLED) to setOf(RequestActor.AUTHOR),
        (RequestStatus.IN_PROGRESS to RequestStatus.COMPLETED) to
            setOf(RequestActor.AUTHOR, RequestActor.HELPER),
        (RequestStatus.IN_PROGRESS to RequestStatus.CANCELLED) to setOf(RequestActor.AUTHOR),
        (RequestStatus.IN_PROGRESS to RequestStatus.OPEN) to
            setOf(RequestActor.AUTHOR, RequestActor.HELPER),
        (RequestStatus.COMPLETED to RequestStatus.OPEN) to setOf(RequestActor.AUTHOR),
        (RequestStatus.CANCELLED to RequestStatus.OPEN) to setOf(RequestActor.AUTHOR)
    )

    fun isTransitionAllowed(from: RequestStatus, to: RequestStatus): Boolean =
        from != to && (from to to) in allowedActors

    fun isActorAllowed(
        from: RequestStatus,
        to: RequestStatus,
        actor: RequestActor
    ): Boolean = allowedActors[from to to]?.contains(actor) == true
}
