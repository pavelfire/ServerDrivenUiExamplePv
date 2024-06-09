package com.vk.directop.serverdrivenuiexamplepv

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime
import java.util.UUID

class MainActivityViewModel : ViewModel() {

    private val realtimeDatabase = Firebase.database
    private val dataNode = realtimeDatabase.getReference("ui/newdata")
    private val layoutNode = realtimeDatabase.getReference("ui/layout")
    private val metaNode = realtimeDatabase.getReference("ui/meta")

    val newDataNode = realtimeDatabase.getReference("ui/newdata")

    fun onAddNewItemClick() {
        val newId = UUID.randomUUID()
        newDataNode.child("$newId").setValue(
            NewsItem(
                id = "\"$newId\"",
                title = "\"On fly added 2\"",
                description = "\"after pressing button added ${LocalDateTime.now()}\"",
                favorite = false,
            )
        )
    }

    fun generateInitialContent() {
        val newId = UUID.randomUUID()
        newDataNode.child("$newId").setValue(
            NewsItem(
                id = "\"$newId\"",
                title = "\"Initial post\"",
                description = "\"This post added into firebase realtime database\"",
                favorite = false,
            )
        )
        layoutNode.child("layout_3").setValue(
            MyLayout(
                columns = 1,
                type = "list"
            )
        )
        layoutNode.child("layout_4").setValue(
            MyLayout(
                columns = 3,
                type = "grid"
            )
        )
        metaNode.setValue(
            Meta(
                canFavorite = false,
                mode = "\"layout_4\""
            )
        )
    }

    //Firebase models
    data class NewsItem(
        val id: String = "",
        val title: String = "",
        val description: String = "",
        val favorite: Boolean = false,
        val favorites: String = "false"
    )

    data class Meta(
        val canFavorite: Boolean = false,
        val mode: String = "",
    )

    data class MyLayout(
        val columns: Int = 2,
        val type: String = "grid",
    )

    //Firebase flows
    private val _dataFlow: Flow<List<NewsItem>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newsItems = snapshot.children.map {
                    Log.d("myTag", "it = $it")
                    it.getValue<NewsItem>()!!
                        .copy(favorite = it.children.find {
                            it.key == "favorite"
                        }!!.getValue<Boolean>()!!.run {
                            return@run this == true
                        })
                }
                trySend(newsItems)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("myTag", "_dataFlow onCanceled")
            }
        }

        dataNode.addValueEventListener(listener)
        awaitClose { dataNode.removeEventListener(listener) }
    }
    private val _layoutTypeMapFlow: Flow<Map<String, LayoutType>> = callbackFlow {
        fun parse(snapshot: DataSnapshot): LayoutType {
            val type = snapshot.children.find {
                it.key == "type"
            }!!.getValue<String>()!!
            return when (type) {
                "list" -> LayoutType.List
                "grid" -> LayoutType.Grid(
                    columns = snapshot.children.find { it.key == "columns" }!!.getValue<Int>()!!
                )

                else -> {
                    Log.e("Unknown type", type)
                    LayoutType.List
                }
            }
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = snapshot.children.associate {
                    it.key!! to parse(it)
                }
                trySend(map)
            }

            override fun onCancelled(error: DatabaseError) {
                //
            }
        }

        layoutNode.addValueEventListener(listener)
        awaitClose { layoutNode.removeEventListener(listener) }
    }
    private val _metaFlow: Flow<Meta> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue<Meta>()!!)
            }

            override fun onCancelled(error: DatabaseError) {
                //
            }
        }

        metaNode.addValueEventListener(listener)
        awaitClose { metaNode.removeEventListener(listener) }
    }

    //ui flow
    val layoutInformationFlow: StateFlow<LayoutInformation?> = combine(
        _dataFlow, _layoutTypeMapFlow, _metaFlow
    ) { newsItems, layoutTypeMap, meta ->
        if (newsItems.isEmpty()) return@combine null
        val layoutMetaInformation = LayoutMeta(
            layoutType = layoutTypeMap[meta.mode] ?: LayoutType.List,
            favoriteEnabled = meta.canFavorite
        )
        return@combine LayoutInformation(
            layoutMeta = layoutMetaInformation,
            layoutData = newsItems
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000L), null)
}
