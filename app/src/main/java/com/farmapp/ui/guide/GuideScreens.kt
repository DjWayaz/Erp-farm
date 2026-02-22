package com.farmapp.ui.guide

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.farmapp.data.local.entity.PestGuideEntity
import com.farmapp.data.repository.PestGuideRepository
import com.farmapp.ui.crop.EmptyState
import com.farmapp.ui.navigation.Screen
import com.farmapp.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val repo: PestGuideRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    @OptIn(ExperimentalCoroutinesApi::class)
    val pests: StateFlow<List<PestGuideEntity>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) repo.getAllPests() else repo.searchPests(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun getPestById(id: String): Flow<PestGuideEntity?> = repo.getPestById(id)
}

// ─── Pest Guide Screen ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PestGuideScreen(navController: NavController, viewModel: GuideViewModel = hiltViewModel()) {
    val pests by viewModel.pests.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📚 Pest & Disease Guide") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrownSurface, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search pests, crops...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Default.Clear, null) } },
                singleLine = true
            )

            if (pests.isEmpty() && query.isNotBlank()) {
                EmptyState(icon = Icons.Default.SearchOff, title = "No Results", subtitle = "Try a different search term")
            } else if (pests.isEmpty()) {
                EmptyState(icon = Icons.Default.MenuBook, title = "Guide Loading...", subtitle = "The pest guide will appear momentarily")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pests) { pest ->
                        PestCard(pest = pest, onClick = { navController.navigate(Screen.GuideDetail.createRoute(pest.id)) })
                    }
                }
            }
        }
    }
}

@Composable
fun PestCard(pest: PestGuideEntity, onClick: () -> Unit) {
    val severityColor = when (pest.severity.uppercase()) {
        "HIGH" -> RedAlert
        "MEDIUM" -> OrangeWarn
        else -> GreenPrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = BrownSurface.copy(alpha = 0.06f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BugReport, null, tint = BrownSurface, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(pest.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                pest.localName?.let { Text("Local: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text("Affects: ${pest.affectedCrop}", style = MaterialTheme.typography.bodySmall)
            }
            Surface(color = severityColor.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                Text(pest.severity, style = MaterialTheme.typography.labelSmall, color = severityColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            Icon(Icons.Default.ChevronRight, null, tint = BrownSurface)
        }
    }
}

// ─── Guide Detail Screen ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideDetailScreen(pestId: String, navController: NavController, viewModel: GuideViewModel = hiltViewModel()) {
    val pest by viewModel.getPestById(pestId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pest?.name ?: "Pest Detail") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrownSurface, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        pest?.let { p ->
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = BrownSurface.copy(alpha = 0.08f))) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(p.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            p.localName?.let { Text("Local Name: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text("Affected Crop: ${p.affectedCrop}", style = MaterialTheme.typography.bodyMedium)
                            val severityColor = when (p.severity.uppercase()) { "HIGH" -> RedAlert; "MEDIUM" -> OrangeWarn; else -> GreenPrimary }
                            Chip(label = p.severity, color = severityColor)
                        }
                    }
                }

                item {
                    GuideSection(title = "🔍 Symptoms", content = p.symptoms, color = OrangeWarn)
                }

                item {
                    GuideSection(title = "💊 Treatment", content = p.treatment, color = SkyBlue)
                }

                p.prevention?.let { prevention ->
                    item {
                        GuideSection(title = "🛡️ Prevention", content = prevention, color = GreenPrimary)
                    }
                }
            }
        } ?: Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun GuideSection(title: String, content: String, color: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun Chip(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
    }
}
