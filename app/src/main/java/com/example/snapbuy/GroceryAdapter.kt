package com.example.snapbuy.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.snapbuy.R
import com.example.snapbuy.models.Grocery

class GroceryAdapter(private val context: Context, private var groceryList: List<Grocery>) :
    RecyclerView.Adapter<GroceryAdapter.GroceryViewHolder>() {

    class GroceryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvGroceryName)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val tvExpiry: TextView = view.findViewById(R.id.tvExpiryDate)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvBarcode: TextView = view.findViewById(R.id.tvBarcode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroceryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_grocery, parent, false)
        return GroceryViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroceryViewHolder, position: Int) {
        val grocery = groceryList[position]
        holder.tvName.text = grocery.name
        holder.tvQuantity.text = "Quantity: ${grocery.quantity}"
        holder.tvExpiry.text = "Expiry: ${grocery.expiryDate}"
        holder.tvCategory.text = "Category: ${grocery.category}"
        holder.tvBarcode.text = "Barcode: ${grocery.barcode}"
    }

    override fun getItemCount(): Int {
        return groceryList.size
    }

    fun updateList(newList: List<Grocery>) {
        groceryList = newList
        notifyDataSetChanged()
    }
}
