package com.rosinante24.kontakappkotlin

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_kontak.*
import kotlinx.android.synthetic.main.content_kontak.*
import kotlinx.android.synthetic.main.input_kontak_dialog.view.*
import kotlinx.android.synthetic.main.kontak_list_item.view.*
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

class KontakActivity : AppCompatActivity(), TextWatcher {

    private lateinit var kontak: ArrayList<Kontak>
    private lateinit var adapter: KontakAdapter
    private lateinit var prefManager: SharedPreferences
    private var entryValid = false
    private lateinit var mFirstNameEdit: EditText
    private lateinit var mLastNameEdit: EditText
    private lateinit var mEmailEdit: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kontak)

        prefManager = getPreferences(Context.MODE_PRIVATE)
        kontak = loadKontak()
        adapter = KontakAdapter(kontak)
        setSupportActionBar(toolbar)
        setupRecyclerView()
        fab.setOnClickListener { showAddKontakDialog(-1) }

    }

    private inner class KontakAdapter internal constructor(
            private val mContacts: java.util.ArrayList<Kontak>) :
            RecyclerView.Adapter<KontakAdapter.ViewHolder>() {

        override fun onCreateViewHolder(
                parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.kontak_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(
                holder: ViewHolder, position: Int) {
            val (firstName, lastName, email) = mContacts[position]
            val fullName = "$firstName $lastName"
            holder.nameLabel.text = fullName
            holder.emailLabel.text = email
        }

        override fun getItemCount(): Int {
            return mContacts.size
        }

        internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var nameLabel: TextView = itemView.textview_name
            var emailLabel: TextView = itemView.textview_email

            init {
                itemView.setOnClickListener { showAddKontakDialog(adapterPosition) }
            }
        }
    }


    @SuppressLint("InflateParams")
    private fun showAddKontakDialog(contactPosition: Int) {
        // Inflates the dialog view
        val dialogView = LayoutInflater.from(this)
                .inflate(R.layout.input_kontak_dialog, null)

        mFirstNameEdit = dialogView.edittext_firstname
        mLastNameEdit = dialogView.edittext_lastname
        mEmailEdit = dialogView.edittext_email

        // Listens to text changes to validate after each key press
        mFirstNameEdit.addTextChangedListener(this)
        mLastNameEdit.addTextChangedListener(this)
        mEmailEdit.addTextChangedListener(this)

        // Checks if the user is editing an existing contact
        val editing = contactPosition > -1

        val dialogTitle = if (editing)
            getString(R.string.edit_contact)
        else
            getString(R.string.new_contact)

        // Builds the AlertDialog and sets the custom view. Pass null for
        // the positive and negative buttons, as you will override the button
        // presses manually to perform validation before closing the dialog
        val builder = AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle(dialogTitle)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)

        val dialog = builder.show()

        // If the contact is being edited, populates the EditText with the old
        // information
        if (editing) {
            val (firstName, lastName, email) = kontak[contactPosition]
            mFirstNameEdit.setText(firstName)
            mFirstNameEdit.isEnabled = false
            mLastNameEdit.setText(lastName)
            mLastNameEdit.isEnabled = false
            mEmailEdit.setText(email)
        }
        // Overrides the "Save" button press and check for valid input
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            // If input is valid, creates and saves the new contact,
            // or replaces it if the contact is being edited
            if (entryValid) {
                if (editing) {
                    val editedContact = kontak[contactPosition]
                    editedContact.email = mEmailEdit.text.toString()
                    kontak[contactPosition] = editedContact
                    adapter.notifyItemChanged(contactPosition)
                } else {
                    val newContact = Kontak(
                            mFirstNameEdit.text.toString(),
                            mLastNameEdit.text.toString(),
                            mEmailEdit.text.toString()
                    )

                    kontak.add(newContact)
                    adapter.notifyItemInserted(kontak.size)
                }
                saveContacts()
                dialog.dismiss()
            } else {
                // Otherwise, shows an error Toast
                Toast.makeText(this@KontakActivity,
                        R.string.contact_not_valid,
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveContacts() {
        val editor = prefManager.edit()
        editor.clear()
        val contactSet = kontak.map { Gson().toJson(it) }.toSet()
        editor.putStringSet(CONTACT_KEY, contactSet)
        editor.apply()
    }

    private fun setupRecyclerView() {
        contact_list.addItemDecoration(DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL))
        contact_list.adapter = adapter

        // Implements swipe to delete
        val helper = ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                    override fun onMove(rV: RecyclerView,
                                        viewHolder: RecyclerView.ViewHolder,
                                        target: RecyclerView.ViewHolder): Boolean {
                        return false
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
                                          direction: Int) {
                        val position = viewHolder.adapterPosition
                        kontak.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        saveContacts()
                    }
                })

        helper.attachToRecyclerView(contact_list)
    }

    private fun loadKontak(): ArrayList<Kontak> {
        val setKontak = prefManager.getStringSet(CONTACT_KEY, HashSet())
        return setKontak.mapTo(ArrayList()) { Gson().fromJson(it, Kontak::class.java) }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_kontak, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        when (id) {
            R.id.action_clear -> {
                clearContacts()
                return true
            }
            R.id.action_generate -> {
                generateContacts()
                return true
            }
            R.id.action_sort_first -> {
                kontak.sortBy { it.firstName }
                adapter.notifyDataSetChanged()
                return true
            }
            R.id.action_sort_last -> {
                kontak.sortBy { it.lastName }
                adapter.notifyDataSetChanged()
                return true
            }
        }

        return super.onOptionsItemSelected(item)

    }

    private fun generateContacts() {
        val contactsString = readContactJsonFile()
        try {
            val contactsJson = JSONArray(contactsString)
            for (i in 0 until contactsJson.length()) {
                val contactJson = contactsJson.getJSONObject(i)
                val contact = Kontak(
                        contactJson.getString("first_name"),
                        contactJson.getString("last_name"),
                        contactJson.getString("email"))
                Log.d(TAG, "generateContacts: " + contact.toString())
                kontak.add(contact)
            }

            adapter.notifyDataSetChanged()
            saveContacts()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun readContactJsonFile(): String? {
        var contactsString: String? = null
        try {
            val inputStream = assets.open("mock_contacts.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()

            contactsString = String(buffer)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return contactsString
    }

    private fun clearContacts() {
        kontak.clear()
        saveContacts()
        adapter.notifyDataSetChanged()
    }

    override fun afterTextChanged(s: Editable?) {
        val notEmpty: TextView.() -> Boolean = { text.isNotEmpty() }
        val isEmail: TextView.() -> Boolean = { Patterns.EMAIL_ADDRESS.matcher(text).matches() }

        entryValid = mFirstNameEdit.validateWith(validator = notEmpty) and
                mLastNameEdit.validateWith(validator = notEmpty) and
                mEmailEdit.validateWith(validator = isEmail)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    companion object {

        private val CONTACT_KEY = "contact_key"
        private val TAG = KontakActivity::class.java.simpleName
    }
}
