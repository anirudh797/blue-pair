/*
 * MIT License
 * <p>
 * Copyright (c) 2017 Donato Rimenti
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package co.aurasphere.scanner.view

import co.aurasphere.scanner.bluetooth.BluetoothController.Companion.deviceToString
import android.bluetooth.BluetoothDevice
import android.support.v7.widget.RecyclerView
import co.aurasphere.scanner.bluetooth.BluetoothDiscoveryDeviceListener
import co.aurasphere.scanner.bluetooth.BluetoothController
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import co.aurasphere.scanner.R
import android.widget.TextView
import java.util.ArrayList

/**
 * [RecyclerView.Adapter] that can display a [BluetoothDevice] and makes a call to the
 * specified [ListInteractionListener] when the element is tapped.
 *
 * @author Donato Rimenti
 */
class DeviceRecyclerViewAdapter(listener: ListInteractionListener<BluetoothDevice?>?) :
    RecyclerView.Adapter<DeviceRecyclerViewAdapter.ViewHolder>(), BluetoothDiscoveryDeviceListener {
    /**
     * The devices shown in this [RecyclerView].
     */
    private val devices: MutableList<BluetoothDevice?>

    /**
     * Callback for handling interaction events.
     */
    private val listener: ListInteractionListener<BluetoothDevice?>?

    /**
     * Controller for Bluetooth functionalities.
     */
    private var bluetooth: BluetoothController? = null

    /**
     * {@inheritDoc}
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_device_item, parent, false)
        return ViewHolder(view)
    }

    /**
     * {@inheritDoc}
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = devices[position]
        holder.mImageView.setImageResource(getDeviceIcon(devices[position]))
        holder.mDeviceNameView.text = devices[position]!!.name
        holder.mDeviceAddressView.text = devices[position]!!.address
        holder.mView.setOnClickListener { listener?.onItemClick(holder.mItem) }
    }

    /**
     * Returns the icon shown on the left of the device inside the list.
     *
     * @param device the device for the icon to get.
     * @return a resource drawable id for the device icon.
     */
    private fun getDeviceIcon(device: BluetoothDevice?): Int {
        return if (bluetooth!!.isAlreadyPaired(device)) {
            R.drawable.ic_bluetooth_connected_black_24dp
        } else {
            R.drawable.ic_bluetooth_black_24dp
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun getItemCount(): Int {
        return devices.size
    }

    /**
     * {@inheritDoc}
     */
    override fun onDeviceDiscovered(device: BluetoothDevice?) {
        listener!!.endLoading(true)
        devices.add(device)
        notifyDataSetChanged()
    }

    /**
     * {@inheritDoc}
     */
    override fun onDeviceDiscoveryStarted() {
        cleanView()
        listener!!.startLoading()
    }

    /**
     * Cleans the view.
     */
    fun cleanView() {
        devices.clear()
        notifyDataSetChanged()
    }

    /**
     * {@inheritDoc}
     */
    override fun setBluetoothController(bluetooth: BluetoothController?) {
        this.bluetooth = bluetooth
    }

    /**
     * {@inheritDoc}
     */
    override fun onDeviceDiscoveryEnd() {
        listener!!.endLoading(false)
    }

    /**
     * {@inheritDoc}
     */
    override fun onBluetoothStatusChanged() {
        // Notifies the Bluetooth controller.
        bluetooth!!.onBluetoothStatusChanged()
    }

    /**
     * {@inheritDoc}
     */
    override fun onBluetoothTurningOn() {
        listener!!.startLoading()
    }

    /**
     * {@inheritDoc}
     */
    override fun onDevicePairingEnded() {
        if (bluetooth!!.isPairingInProgress) {
            val device = bluetooth!!.boundingDevice
            when (bluetooth!!.pairingDeviceStatus) {
                BluetoothDevice.BOND_BONDING -> {}
                BluetoothDevice.BOND_BONDED -> {
                    // Successfully paired.
                    listener!!.endLoadingWithDialog(false, device)

                    // Updates the icon for this element.
                    notifyDataSetChanged()
                }
                BluetoothDevice.BOND_NONE ->                     // Failed pairing.
                    listener!!.endLoadingWithDialog(true, device)
            }
        }
    }

    /**
     * ViewHolder for a BluetoothDevice.
     */
    inner class ViewHolder(
        /**
         * The inflated view of this ViewHolder.
         */
        val mView: View
    ) : RecyclerView.ViewHolder(mView) {
        /**
         * The icon of the device.
         */
        val mImageView: ImageView

        /**
         * The name of the device.
         */
        val mDeviceNameView: TextView

        /**
         * The MAC address of the BluetoothDevice.
         */
        val mDeviceAddressView: TextView

        /**
         * The item of this ViewHolder.
         */
        var mItem: BluetoothDevice? = null

        /**
         * {@inheritDoc}
         */
        override fun toString(): String {
            return super.toString() + " '" + deviceToString(mItem!!) + "'"
        }

        /**
         * Instantiates a new ViewHolder.
         *
         * @param view the inflated view of this ViewHolder.
         */
        init {
            mImageView = mView.findViewById<View>(R.id.device_icon) as ImageView
            mDeviceNameView = mView.findViewById<View>(R.id.device_name) as TextView
            mDeviceAddressView = mView.findViewById<View>(R.id.device_address) as TextView
        }
    }

    /**
     * Instantiates a new DeviceRecyclerViewAdapter.
     *
     * @param listener an handler for interaction events.
     */
    init {
        devices = ArrayList()
        this.listener = listener
    }
}