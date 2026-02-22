package com.example.semsync

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.semsync.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGreeting()
        fetchTodaySchedule()
        fetchTasksDueSoon()
        setupRecyclerViews()
    }

    private fun setupGreeting() {
        val user = auth.currentUser
        val name = user?.displayName?.split(" ")?.firstOrNull() ?: "Student"
        val fullGreeting = "${getGreeting()} $name! Here's what's happening today."
        binding.tvGreeting.text = fullGreeting
    }

    private fun fetchTodaySchedule() {
        val userId = auth.currentUser?.uid ?: return
        val today = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()).toLowerCase(Locale.getDefault())

        db.collection("users").document(userId).collection("timetable")
            .whereEqualTo("dayOfWeek", today)
            .orderBy("startTime")
            .get()
            .addOnSuccessListener { documents ->
                val now = Calendar.getInstance().time
                val upcomingClass = documents.map { it.toObject(TimetableEntry::class.java) }
                    .firstOrNull { 
                        it.endTime?.let { end -> Date(end).after(now) } == true 
                    }

                if (upcomingClass != null) {
                    binding.cardTodaysSchedule.visibility = View.VISIBLE
                    binding.tvScheduleUnitName.text = upcomingClass.unitName
                    binding.tvScheduleLocation.text = upcomingClass.location
                    binding.tvScheduleUnitCode.text = upcomingClass.unitCode
                    val startTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(upcomingClass.startTime ?: 0))
                    val endTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(upcomingClass.endTime ?: 0))
                    binding.tvScheduleTime.text = "$startTime\nto\n$endTime"
                } else {
                    binding.cardTodaysSchedule.visibility = View.VISIBLE
                    binding.tvScheduleUnitName.text = "No classes scheduled"
                    binding.tvScheduleLocation.text = ""
                    binding.tvScheduleUnitCode.text = ""
                    binding.tvScheduleTime.text = ""
                }
            }
    }

    private fun fetchTasksDueSoon() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("tasks")
            .whereEqualTo("userId", userId)
            .whereEqualTo("completed", false)
            .orderBy("dueDate", Query.Direction.ASCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val task = documents.documents[0].toObject(Task::class.java)
                    if (task != null && task.dueDate != null) {
                        binding.cardTasksDueSoon.visibility = View.VISIBLE
                        binding.tvTaskTitle.text = task.title
                        // Calculate days until due or overdue
                        val diff = task.dueDate - Calendar.getInstance().time.time
                        val days = diff / (1000 * 60 * 60 * 24)
                        binding.tvTaskDueDate.text = when {
                            days < -1 -> "${-days} days ago"
                            days == -1L -> "Yesterday"
                            days == 0L -> "Today"
                            days == 1L -> "Tomorrow"
                            else -> "in $days days"
                        }
                    } else {
                         // Task exists but might be missing dueDate or parsing failed
                         binding.cardTasksDueSoon.visibility = View.GONE
                    }
                } else {
                    // No tasks found
                    binding.cardTasksDueSoon.visibility = View.GONE
                }
            }
    }

    private fun setupRecyclerViews() {
        binding.recyclerRecentNotes.layoutManager = LinearLayoutManager(context)
        binding.recyclerRecentNotes.adapter = NotesAdapter(emptyList())

        binding.recyclerAnnouncements.layoutManager = LinearLayoutManager(context)
        binding.recyclerAnnouncements.adapter = AnnouncementsAdapter(emptyList())
    }


    private fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            else -> "Good evening,"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Placeholder Adapters for RecyclerViews with corrected structure
class NotesAdapter(private val items: List<Any>) : RecyclerView.Adapter<NotesAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = TextView(parent.context)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
    override fun getItemCount() = items.size
}

class AnnouncementsAdapter(private val items: List<Any>) : RecyclerView.Adapter<AnnouncementsAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = TextView(parent.context)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
    override fun getItemCount() = items.size
}