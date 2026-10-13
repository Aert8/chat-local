package com.example.modelolocal.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val dao: ChatDao) {

    fun getAllChats(): Flow<List<ChatEntity>> = dao.getAllChats()

    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>> =
        dao.getMessagesForChat(chatId)

    suspend fun getMessagesForChatOnce(chatId: Long): List<MessageEntity> =
        dao.getMessagesForChatOnce(chatId)

    suspend fun createChat(title: String): Long {
        val now = System.currentTimeMillis()
        return dao.insertChat(ChatEntity(title = title, createdAt = now, updatedAt = now))
    }

    suspend fun addMessage(chatId: Long, role: String, content: String) {
        val now = System.currentTimeMillis()
        dao.insertMessage(MessageEntity(chatId = chatId, role = role, content = content, timestamp = now))
        dao.touchChat(chatId, updatedAt = now)
    }

    suspend fun updateChatTitle(chatId: Long, title: String) {
        dao.updateChat(chatId, title = title, updatedAt = System.currentTimeMillis())
    }

    suspend fun deleteChat(chatId: Long) {
        dao.deleteChat(chatId)
    }
}
