package com.example.realkick.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Team(
    val name: String,
    val country: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val emoji: String,
    val description: String
)

val TEAMS_LIST = listOf(
    Team("Real Madrid", "España", Color(0xFFFFFFFF), Color(0xFF8E8E93), "👑", "Mística, señorío y grandeza mundial. El rey de Europa."),
    Team("Francia", "Selección", Color(0xFF0B2265), Color(0xFFE42518), "🇫🇷", "Les Bleus. Fuerza, velocidad y talento de clase mundial."),
    Team("Argentina", "Selección", Color(0xFF75AADB), Color(0xFFFFFFFF), "🏆", "La albiceleste. Pasión inigualable, cuna de campeones."),
    Team("Chivas", "México", Color(0xFFE30613), Color(0xFF002855), "🐐", "El Rebaño Sagrado. Tradición, identidad y orgullo mexicano."),
    Team("Club América", "México", Color(0xFFFADE4B), Color(0xFF002F6C), "🦅", "Las Águilas. Grandeza, poderío y el club más ganador de México.")
)

data class HistoryItem(
    val id: String,
    val name: String,
    val year: String,
    val emoji: String,
    val description: String
)

val HISTORY_ITEMS = listOf(
    HistoryItem("copa_del_mundo", "Copa del Mundo", "Desde 1974", "🏆", "El trofeo más codiciado del planeta. Diseñado por Silvio Gazzaniga, representa la gloria máxima del deporte."),
    HistoryItem("mascota_2026", "Mascota 2026", "Mundial 2026", "🦁", "Representación de la unidad y diversidad de los tres países anfitriones (México, EE. UU. y Canadá) para el Mundial 2026."),
    HistoryItem("primer_balon", "El Primer Balón", "Uruguay 1930", "⚽", "Réplica de cuero con cordones del balón modelo T-Shape utilizado en la primera final de la Copa del Mundo."),
    HistoryItem("botin_de_oro", "Botín de Oro", "Desde 1982", "🥾", "Premio otorgado al máximo goleador de cada Copa del Mundo. Un símbolo de precisión y letalidad."),
    HistoryItem("silbato_1930", "Silbato Histórico", "Uruguay 1930", "📢", "Réplica del silbato de John Langenus utilizado en la histórica primera final del mundo en Montevideo.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamSelectionScreen(
    onTeamSelected: (String) -> Unit,
    onHistorySelected: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Penales, 1 = Historia
    var selectedTeam by remember { mutableStateOf<Team?>(null) }
    var selectedHistoryItem by remember { mutableStateOf<HistoryItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF000000),
                        Color(0xFF0A0A0A),
                        Color(0xFF121212)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "RealKick AR",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )
            
            Text(
                text = "Experimenta el fútbol en realidad aumentada",
                fontSize = 16.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            // Selector de pestaña minimalista
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F0F12))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Tanda de Penales", "Historia del Fútbol").forEachIndexed { index, label ->
                    val isSelected = activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { activeTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activeTab == 0) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(TEAMS_LIST) { team ->
                        TeamCard(
                            team = team,
                            isSelected = selectedTeam == team,
                            onClick = { selectedTeam = team }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = selectedTeam != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
                ) {
                    selectedTeam?.let { team ->
                        Button(
                            onClick = { onTeamSelected(team.name) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(28.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = "JUGAR CON ${team.name.uppercase()} ${team.emoji}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(HISTORY_ITEMS) { item ->
                        HistoryCard(
                            item = item,
                            isSelected = selectedHistoryItem == item,
                            onClick = { selectedHistoryItem = item }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = selectedHistoryItem != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
                ) {
                    selectedHistoryItem?.let { item ->
                        Button(
                            onClick = { onHistorySelected(item.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(28.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = "VER EN REALIDAD AUMENTADA ${item.emoji}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamCard(
    team: Team,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1A1A1E) else Color(0xFF0F0F12)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 0.dp
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, Color.White)
        } else {
            BorderStroke(1.dp, Color(0xFF1E1E22))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = team.emoji,
                    fontSize = 28.sp
                )
                
                // Color badges
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(team.primaryColor)
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(team.secondaryColor)
                    )
                }
            }

            Column {
                Text(
                    text = team.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = team.country,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = team.description,
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun HistoryCard(
    item: HistoryItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1A1A1E) else Color(0xFF0F0F12)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 0.dp
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, Color.White)
        } else {
            BorderStroke(1.dp, Color(0xFF1E1E22))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.emoji,
                    fontSize = 28.sp
                )
                Text(
                    text = item.year,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            Column {
                Text(
                    text = item.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    maxLines = 3,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
