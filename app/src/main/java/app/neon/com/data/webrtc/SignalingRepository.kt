package app.neon.com.data.webrtc

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import javax.inject.Inject
import javax.inject.Singleton

sealed class SignalingEvent {
    data class OfferReceived(val sdp: SessionDescription) : SignalingEvent()
    data class AnswerReceived(val sdp: SessionDescription) : SignalingEvent()
    data class IceCandidateReceived(val candidate: IceCandidate) : SignalingEvent()
    object CallEnded : SignalingEvent()
}

@Singleton
class SignalingRepository @Inject constructor() {

    private val db = FirebaseDatabase.getInstance()
    private val signalingRef = db.getReference("signaling")

    /**
     * Mengirimkan WebRTC Offer SDP ke path Firebase /signaling/{chatId}/offer
     */
    fun sendOffer(chatId: String, sdp: SessionDescription, callerId: String) {
        val offerMap = hashMapOf<String, Any>(
            "type" to "offer",
            "sdp" to sdp.description,
            "senderId" to callerId,
            "timestamp" to ServerValue.TIMESTAMP
        )
        signalingRef.child(chatId).child("offer").setValue(offerMap)
        signalingRef.child(chatId).child("status").setValue("ringing")
    }

    /**
     * Mengirimkan WebRTC Answer SDP ke path Firebase /signaling/{chatId}/answer
     */
    fun sendAnswer(chatId: String, sdp: SessionDescription, receiverId: String) {
        val answerMap = hashMapOf<String, Any>(
            "type" to "answer",
            "sdp" to sdp.description,
            "senderId" to receiverId,
            "timestamp" to ServerValue.TIMESTAMP
        )
        signalingRef.child(chatId).child("answer").setValue(answerMap)
        signalingRef.child(chatId).child("status").setValue("connected")
    }

    /**
     * Mengirimkan ICE Candidate ke path Firebase /signaling/{chatId}/candidates
     */
    fun sendIceCandidate(chatId: String, candidate: IceCandidate, senderId: String) {
        val candidateMap = hashMapOf<String, Any>(
            "sdpMid" to (candidate.sdpMid ?: ""),
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "sdpCandidate" to candidate.sdp,
            "senderId" to senderId
        )
        signalingRef.child(chatId).child("candidates").push().setValue(candidateMap)
    }

    /**
     * Mendengarkan event signaling SDP Offer/Answer dan ICE Candidate dari lawan bicara
     */
    fun listenSignaling(chatId: String, currentUserId: String): Flow<SignalingEvent> = callbackFlow {
        val targetChatRef = signalingRef.child(chatId)

        // 1. Listen Offer & Answer & Status
        val valueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.child("status").getValue(String::class.java)
                if (status == "ended") {
                    trySend(SignalingEvent.CallEnded)
                    return
                }

                // Cek Offer jika bukan dikirim oleh diri sendiri
                val offerSnapshot = snapshot.child("offer")
                val offerSender = offerSnapshot.child("senderId").getValue(String::class.java)
                val offerSdp = offerSnapshot.child("sdp").getValue(String::class.java)
                if (offerSdp != null && offerSender != null && offerSender != currentUserId) {
                    val desc = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
                    trySend(SignalingEvent.OfferReceived(desc))
                }

                // Cek Answer jika bukan dikirim oleh diri sendiri
                val answerSnapshot = snapshot.child("answer")
                val answerSender = answerSnapshot.child("senderId").getValue(String::class.java)
                val answerSdp = answerSnapshot.child("sdp").getValue(String::class.java)
                if (answerSdp != null && answerSender != null && answerSender != currentUserId) {
                    val desc = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
                    trySend(SignalingEvent.AnswerReceived(desc))
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        targetChatRef.addValueEventListener(valueListener)

        // 2. Listen ICE Candidates
        val candidatesRef = targetChatRef.child("candidates")
        val candidateListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val senderId = snapshot.child("senderId").getValue(String::class.java)
                if (senderId != null && senderId != currentUserId) {
                    val sdpMid = snapshot.child("sdpMid").getValue(String::class.java) ?: ""
                    val sdpMLineIndex = snapshot.child("sdpMLineIndex").getValue(Int::class.java) ?: 0
                    val sdpCandidate = snapshot.child("sdpCandidate").getValue(String::class.java) ?: ""

                    if (sdpCandidate.isNotBlank()) {
                        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdpCandidate)
                        trySend(SignalingEvent.IceCandidateReceived(candidate))
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        candidatesRef.addChildEventListener(candidateListener)

        awaitClose {
            targetChatRef.removeEventListener(valueListener)
            candidatesRef.removeEventListener(candidateListener)
        }
    }

    /**
     * Mengakhiri panggilan dan membersihkan signaling session
     */
    fun endCall(chatId: String) {
        signalingRef.child(chatId).child("status").setValue("ended")
        signalingRef.child(chatId).removeValue()
    }
}
