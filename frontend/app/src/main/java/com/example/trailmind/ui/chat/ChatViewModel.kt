package com.example.trailmind.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.remote.MlClient
import com.example.trailmind.data.remote.dto.ChatMsg
import com.example.trailmind.data.remote.dto.ChatRequest
import com.example.trailmind.data.remote.dto.TurnResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow(
        listOf(
            ChatMsg(
                "assistant",
                "Namaste! I'm TrailMind. Tell me about the trip you have in mind and I'll plan it for you, or ask me anything about Nepal travel. Not sure what to ask? Try one of these:"
            )
        )
    )
    val messages: StateFlow<List<ChatMsg>> = _messages

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || _loading.value) return
        _messages.value = _messages.value + ChatMsg("user", t)
        _loading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            // The greeting and error bubbles are not sent to the server
            val all = _messages.value.drop(1).filter { it.role == "user" || it.role == "assistant" }
            // Plain copies: the plan card itself is never sent
            val plain = all.map { ChatMsg(it.role, it.text) }
            // Trip details are read only from messages after the last plan
            val lastPlan = all.indexOfLast { it.plan != null }
            val sinceLastPlan = plain.drop(lastPlan + 1)

            val turn = askTurn(sinceLastPlan)
            when (turn?.type) {
                "question", "limit" -> add(ChatMsg("assistant", turn.message ?: ""))
                "plan" -> add(ChatMsg("assistant", summary(turn), turn))
                "route", "hotels" -> add(ChatMsg("assistant", cardSummary(turn), turn))
                else -> streamChat(plain)   // normal chat, or /chat/turn failed
            }
            _loading.value = false
        }
    }

    private suspend fun askTurn(history: List<ChatMsg>): TurnResponse? {
        return try {
            val res = MlClient.mlApi.chatTurn(ChatRequest(history))
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    // Short text version of the plan, so later chat answers know about it
    private fun summary(t: TurnResponse): String {
        val i = t.inputs
        val total = t.budget?.itemized_total_npr ?: 0.0
        return "Trip plan for ${i?.destination}: ${i?.days} days, ${i?.group_size} people, " +
                "estimated NPR ${"%,.0f".format(total)}."
    }
    // Short text for the history, so later chat answers know a card was shown
    private fun cardSummary(t: TurnResponse): String {
        val i = t.inputs
        return if (t.type == "route") "Route from ${i?.start_city} to ${i?.destination} shown."
        else "Hotels near ${i?.destination} shown."
    }

    private suspend fun streamChat(history: List<ChatMsg>) {
        try {
            val res = MlClient.mlApi.chatStream(ChatRequest(history))
            val body = res.body()
            if (!res.isSuccessful || body == null) {
                add(ChatMsg("error", "The assistant is busy right now. Please try again in a moment."))
            } else {
                body.use { b ->
                    val reader = b.charStream()
                    val buf = CharArray(256)
                    val sb = StringBuilder()
                    var started = false
                    while (true) {
                        val n = reader.read(buf)
                        if (n < 0) break
                        sb.append(buf, 0, n)
                        val shown = ChatMsg("assistant", tidy(sb.toString()))
                        // first chunk adds the bubble, later chunks grow it
                        _messages.value =
                            if (started) _messages.value.dropLast(1) + shown
                            else _messages.value + shown
                        started = true
                    }
                    if (!started) add(ChatMsg("error", "The assistant sent an empty reply. Please try again."))
                }
            }
        } catch (e: Exception) {
            add(ChatMsg("error", "Could not reach the server. Check your connection and try again."))
        }
    }

    private fun add(m: ChatMsg) {
        _messages.value = _messages.value + m
    }

    // The screen shows plain text, so remove markdown symbols
    private fun tidy(s: String) = s.replace("**", "").replace("\n* ", "\n• ").replace("\n- ", "\n• ")
}