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
        fetchAnnouncements()
        setupRecyclerViews()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnViewCalendar.setOnClickListener {
            // Navigate to Timetable
            val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
            bottomNav.selectedItemId = R.id.navigation_timetable
        }
        
        binding.btnViewTasks.setOnClickListener {
            // Navigate to Tasks
            val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
            bottomNav.selectedItemId = R.id.navigation_tasks
        }
        
        binding.btnViewNotes.setOnClickListener {
            // Navigate to Notebook
            val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
            bottomNav.selectedItemId = R.id.navigation_notebook
        }

        binding.btnNotifications.setOnClickListener {
           // TODO: Notifications screen
        }

        binding.btnProfile.setOnClickListener {
            // TODO: Profile screen
        }
    }

    private fun setupGreeting() {
        val user = auth.currentUser
        val name = user?.displayName?.split(" ")?.firstOrNull() ?: "Student"
        val fullGreeting = "${getGreeting()} $name! Here's what's happening today."
        binding.tvGreeting.text = fullGreeting
    }

    private fun fetchTodaySchedule() {
        val userId = auth.currentUser?.uid ?: return
        val calendar = Calendar.getInstance()
        val todayIndex = calendar.get(Calendar.DAY_OF_WEEK) // Sunday=1
        val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val todayString = days[todayIndex - 1]
        
        // Calculate tomorrow's index for fallback
        val tomorrowIndexRaw = (todayIndex % 7) + 1
        val tomorrowString = days[tomorrowIndexRaw - 1]

        db.collection("groups")
            .whereArrayContains("members", userId)
            .get()
            .addOnSuccessListener { groupDocs ->
                val todayUnits = mutableListOf<TimetableEntry>()
                val tomorrowUnits = mutableListOf<TimetableEntry>()
                // Track pending fetches
                var pendingFetches = groupDocs.size()
                
                if (pendingFetches == 0) {
                     updateDashboardSchedule(emptyList(), emptyList())
                     return@addOnSuccessListener
                }

                for (groupDoc in groupDocs) {
                    val groupName = groupDoc.getString("name") ?: ""
                    groupDoc.reference.collection("units")
                        .get()
                        .addOnSuccessListener { unitDocs ->
                            for (unitDoc in unitDocs) {
                                val unit = unitDoc.toObject(AcademicUnit::class.java)
                                for (schedule in unit.schedule) {
                                    // Check Today
                                    if (schedule.day.equals(todayString, ignoreCase = true)) {
                                        todayUnits.add(
                                            TimetableEntry(
                                                unitName = unit.name,
                                                unitCode = unit.code,
                                                location = schedule.location,
                                                startTime = schedule.startTime,
                                                endTime = schedule.endTime,
                                                dayOfWeek = todayIndex - 1
                                            )
                                        )
                                    }
                                    // Check Tomorrow
                                    if (schedule.day.equals(tomorrowString, ignoreCase = true)) {
                                        tomorrowUnits.add(
                                            TimetableEntry(
                                                unitName = unit.name,
                                                unitCode = unit.code,
                                                location = schedule.location,
                                                startTime = schedule.startTime,
                                                endTime = schedule.endTime,
                                                dayOfWeek = tomorrowIndexRaw - 1
                                            )
                                        )
                                    }
                                }
                            }
                            pendingFetches--
                            if (pendingFetches == 0) {
                                updateDashboardSchedule(todayUnits, tomorrowUnits)
                            }
                        }
                        .addOnFailureListener {
                            pendingFetches--
                            if (pendingFetches == 0) {
                                updateDashboardSchedule(todayUnits, tomorrowUnits)
                            }
                        }
                }
            }
            .addOnFailureListener {
                binding.cardTodaysSchedule.visibility = View.GONE
            }
    }

    private fun updateDashboardSchedule(todayUnits: List<TimetableEntry>, tomorrowUnits: List<TimetableEntry>) {
        val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val calendar = Calendar.getInstance()
        val todayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val todayName = days[todayIndex]
        
        // Sort by start time
        val sortedToday = todayUnits.sortedBy { it.startTime }
        val sortedTomorrow = tomorrowUnits.sortedBy { it.startTime }

        // Find the first upcoming class today (endTime > nowTime)
        val upcomingClass = sortedToday.firstOrNull { 
            it.endTime > nowTime
        }

        binding.cardTodaysSchedule.visibility = View.VISIBLE

        if (upcomingClass != null) {
            // Case 1: Active class today
            binding.tvScheduleTitle.text = "Today's Schedule ($todayName)"
            binding.tvScheduleUnitName.text = upcomingClass.unitName
            binding.tvScheduleLocation.text = upcomingClass.location
            binding.tvScheduleUnitCode.text = upcomingClass.unitCode
            binding.tvScheduleTime.text = "${upcomingClass.startTime} - ${upcomingClass.endTime}"
            binding.tvScheduleGroup.text = "Today"
        } else if (sortedTomorrow.isNotEmpty()) {
            // Case 2: No more classes today, show tomorrow
            val nextClass = sortedTomorrow.first()
            val tomorrowIndex = (todayIndex + 1) % 7
            val tomorrowName = days[tomorrowIndex]
            
            binding.tvScheduleTitle.text = "Tomorrow's Schedule ($tomorrowName)"
            binding.tvScheduleUnitName.text = nextClass.unitName
            binding.tvScheduleLocation.text = nextClass.location
            binding.tvScheduleUnitCode.text = nextClass.unitCode
            binding.tvScheduleTime.text = "${nextClass.startTime} - ${nextClass.endTime}"
            binding.tvScheduleGroup.text = "Tomorrow"
        } else {
            // Case 3: No classes today or tomorrow
            binding.tvScheduleTitle.text = "Schedule"
            binding.tvScheduleUnitName.text = "No upcoming classes"
            binding.tvScheduleLocation.text = ""
            binding.tvScheduleUnitCode.text = ""
            binding.tvScheduleTime.text = ""
            binding.tvScheduleGroup.text = "-"
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

    private fun fetchAnnouncements() {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("groups")
            .whereArrayContains("members", userId)
            .get()
            .addOnSuccessListener { groupDocs ->
                val allPosts = mutableListOf<EnrichedPost>()
                var pendingFetches = groupDocs.size()

                if (pendingFetches == 0) {
                     updateAnnouncementsUI(emptyList())
                     return@addOnSuccessListener
                }

                for (groupDoc in groupDocs) {
                    val groupName = groupDoc.getString("name") ?: "Group"
                    
                    groupDoc.reference.collection("posts")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(3)
                        .get()
                        .addOnSuccessListener { postDocs ->
                            if (_binding == null) return@addOnSuccessListener
                            for (postDoc in postDocs) {
                                val post = postDoc.toObject(GroupPost::class.java)
                                allPosts.add(EnrichedPost(post, groupName))
                            }
                            pendingFetches--
                            if (pendingFetches == 0) {
                                updateAnnouncementsUI(allPosts)
                            }
                        }
                        .addOnFailureListener {
                            pendingFetches--
                            if (pendingFetches == 0) {
                                updateAnnouncementsUI(allPosts)
                            }
                        }
                }
            }
            .addOnFailureListener {
                if (_binding != null) {
                    binding.recyclerAnnouncements.visibility = View.GONE
                }
            }
    }

    private fun updateAnnouncementsUI(posts: List<EnrichedPost>) {
        if (_binding == null) return
        
        // Sort manually since we fetched from multiple collections
        val sortedPosts = posts.sortedByDescending { 
            // Handle Timestamp conversion safely
            it.post.createdAt?.toDate()?.time ?: 0L 
        }.take(5) // Show top 5
            
        if (sortedPosts.isNotEmpty()) {
            binding.recyclerAnnouncements.visibility = View.VISIBLE
            binding.recyclerAnnouncements.adapter = AnnouncementsAdapter(sortedPosts)
        } else {
             // For now, let's just leave it empty or show a placeholder item
             binding.recyclerAnnouncements.visibility = View.GONE
             binding.recyclerAnnouncements.adapter = AnnouncementsAdapter(emptyList())
        }
    }

    data class EnrichedPost(val post: GroupPost, val groupName: String)

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

class AnnouncementsAdapter(private val items: List<HomeFragment.EnrichedPost>) : RecyclerView.Adapter<AnnouncementsAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val groupName: TextView = view.findViewById(R.id.text_group_name)
        val authorName: TextView = view.findViewById(R.id.text_author_name)
        val postTime: TextView = view.findViewById(R.id.text_post_time)
        val content: TextView = view.findViewById(R.id.text_post_content)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_announcement_preview, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.groupName.text = item.groupName
        holder.authorName.text = item.post.authorName
        holder.content.text = item.post.content
        
        // Time formatting
        // Convert timestamp to milliseconds
        val createdTime = item.post.createdAt?.toDate()?.time ?: 0L
        val timeDiff = System.currentTimeMillis() - createdTime
        val seconds = timeDiff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        holder.postTime.text = when {
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }
    
    override fun getItemCount() = items.size
}