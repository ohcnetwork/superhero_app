package kerala.superhero.app.signup

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dialog_searchable_list.*
import kerala.superhero.app.R

class SearchableListDialog(private val activity: Activity, private val searchableList: List<SearchableListItem>, val onItemSelect: (item: SearchableListItem) -> Unit): Dialog(activity) {

    data class SearchableListItem(val id: Any, val name: String)

    class ViewHolder(private val rootView: View): RecyclerView.ViewHolder(rootView){
        fun bind(
            item: SearchableListItem,
            onItemSelect: (item: SearchableListItem) -> Unit
        ){
            val searchResultListItemTextView = rootView.findViewById<TextView>(R.id.searchResultListItemTextView)
            searchResultListItemTextView.text = item.name
            searchResultListItemTextView.setOnClickListener {
                onItemSelect(item)
            }
        }
    }

    private val searchResultList: MutableLiveData<List<SearchableListItem>> = MutableLiveData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_searchable_list)

        searchResultList.value = searchableList

        searchResultRecyclerview.layoutManager = LinearLayoutManager(activity)

        searchResultList.observe(activity as LifecycleOwner, Observer {
            searchResultRecyclerview.adapter = object : RecyclerView.Adapter<ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                    val layoutInflater: LayoutInflater =
                        activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    return ViewHolder(
                        layoutInflater.inflate(
                            R.layout.search_result_item,
                            parent,
                            false
                        )
                    )
                }

                override fun getItemCount(): Int = it.size

                override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                    holder.bind(it[position]){ item ->
                        onItemSelect(item)
                        searchView.clearFocus()
                        dismiss()
                    }
                }

            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchResultList.value = searchableList.filter {
                    it.name.startsWith(query ?: "", true)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchResultList.value = searchableList.filter {
                    it.name.startsWith(newText ?: "", true)
                }
                return true
            }
        })

    }

}