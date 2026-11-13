package ru.vlyashuk.roadbuddy.domain.model

import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.TimestampSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

/**
 * Serializes [Instant] as a Firestore [Timestamp] so `createdAt`/`updatedAt`
 * can be used in `orderBy`/`where` queries. Falls back to parsing legacy
 * ISO-8601 strings when reading documents written before this change.
 */
object InstantTimestampSerializer : KSerializer<Instant> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("InstantTimestamp", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        val millis = value.toEpochMilliseconds()
        val timestamp = Timestamp(
            seconds = millis / 1000,
            nanoseconds = ((millis % 1000) * 1_000_000).toInt()
        )
        encoder.encodeSerializableValue(TimestampSerializer, timestamp)
    }

    override fun deserialize(decoder: Decoder): Instant {
        val timestamp = try {
            decoder.decodeSerializableValue(TimestampSerializer)
        } catch (e: Exception) {
            return Instant.parse(decoder.decodeString())
        }
        val millis = timestamp.seconds * 1000 + timestamp.nanoseconds / 1_000_000
        return Instant.fromEpochMilliseconds(millis)
    }
}
