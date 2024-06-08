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

class MainActivityViewModel : ViewModel() {

    private val realtimeDatabase = Firebase.database
    private val dataNode = realtimeDatabase.getReference("ui/data")
    private val layoutNode = realtimeDatabase.getReference("ui/layout")
    private val metaNode = realtimeDatabase.getReference("ui/meta")

//    val myRef = realtimeDatabase.getReference("message")

    init {
        //myRef.setValue("Hello, World! PV 080624")

//        myRef.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                // This method is called once with the initial value and again
//                // whenever data at this location is updated.
//                val value = dataSnapshot.getValue<String>()
//                Log.d("myTag", "Value is: $value")
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                // Failed to read value
//                Log.w("myTag", "Failed to read value.", error.toException())
//            }
//        })
    }

    //Firebase models
    data class NewsItem(
        val id: String = "",
        val title: String = "",
        val description: String = "",
        val isFavorite: Boolean = false,
    )

    data class Meta(
        val canFavorite: Boolean = false,
        val mode: String = "",
    )

    //Firebase flows
    private val _dataFlow: Flow<List<NewsItem>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newsItems = snapshot.children.map {
                    it.getValue<NewsItem>()!!
                        .copy(isFavorite = it.children.find {
                            it.key == "isFavorite"
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
