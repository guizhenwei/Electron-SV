package org.electroncash.electroncash3

import android.arch.lifecycle.Observer
import android.content.ClipboardManager
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.chaquo.python.PyObject
import kotlinx.android.synthetic.main.addresses.*


val modAddresses = py.getModule("electroncash_gui.android.addresses")
val clsAddress = libMod["address"]!!["Address"]!!


class AddressesFragment : MainFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        title.value = getString(R.string.addresses)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.addresses, menu)
        menu.findItem(R.id.menuFormat).isChecked =
            clsAddress["FMT_UI"] == clsAddress["FMT_LEGACY"]
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (daemonModel.wallet == null) {
            menu.clear()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuFormat -> {
                item.isChecked = !item.isChecked
                clsAddress.callAttr("show_cashaddr", !item.isChecked)
                rvAddresses.adapter?.notifyDataSetChanged()
            }
            else -> throw Exception("Unknown item $item")
        }
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.addresses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rvAddresses.layoutManager = LinearLayoutManager(activity)
        rvAddresses.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        daemonModel.addresses.observe(this, Observer { addresses ->
            rvAddresses.adapter =
                if (addresses == null) null
                else AddressesAdapter(daemonModel.wallet!!, addresses)

            subtitle.value = getString(when {
                addresses == null -> R.string.no_wallet
                rvAddresses.adapter.itemCount == 0 -> R.string.generating_your
                else -> R.string.touch_to_copy
            })
        })
    }
}


class AddressesAdapter(val wallet: PyObject, val addresses: PyObject)
    : BoundAdapter<AddressModel>(R.layout.address) {

    override fun getItem(position: Int): AddressModel {
        return AddressModel(wallet, addresses.callAttr("__getitem__", position))
    }

    override fun getItemCount(): Int {
        return addresses.callAttr("__len__").toJava(Int::class.java)
    }

    override fun onBindViewHolder(holder: BoundViewHolder<AddressModel>, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.itemView.setOnClickListener {
            val addrString = holder.item.addrString
            (getSystemService(ClipboardManager::class)).text =
                if (clsAddress["FMT_UI"] == clsAddress["FMT_LEGACY"]) addrString
                else "bitcoincash:" + addrString
            toast(R.string.address_copied)
        }
    }
}

class AddressModel(val wallet: PyObject, val addr: PyObject) {
    val type
        get() = modAddresses.callAttr("addr_type", wallet, addr).toJava(Int::class.java)

    val addrString
        get() = addr.callAttr("to_ui_string").toString()
}