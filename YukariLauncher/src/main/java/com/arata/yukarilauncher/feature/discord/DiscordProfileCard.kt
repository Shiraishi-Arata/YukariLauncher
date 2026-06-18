package com.arata.yukarilauncher.feature.discord

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arata.yukarilauncher.R

private const val DEFAULT_BANNER = "https://discord.com/assets/97ac61a0b98fd6f01b4de370c9ccdb56.png"

@Composable
fun DiscordProfileCard(
    account: DiscordAccount?,
    customStatus: String?,
    isSelected: Boolean,
    showLogin: Boolean = false,
    onLoginClick: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val borderColors = listOf(primary, primary.copy(alpha = 0.7f))
    val bgColors = listOf(surface, surface)

    Card(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(brush = Brush.verticalGradient(colors = bgColors)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(4.dp, Brush.verticalGradient(colors = borderColors)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box {
            AsyncImage(
                model = account?.bannerUrl ?: DEFAULT_BANNER,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.FillWidth,
                placeholder = painterResource(R.drawable.ic_discord),
                error = painterResource(R.drawable.ic_discord)
            )

            AsyncImage(
                model = account?.avatarUrl ?: R.drawable.ic_discord,
                placeholder = painterResource(R.drawable.ic_discord),
                error = painterResource(R.drawable.ic_discord),
                contentDescription = null,
                modifier = Modifier
                    .padding(16.dp, 64.dp, 16.dp, 6.dp)
                    .size(110.dp)
                    .border(width = 8.dp, color = primary, shape = CircleShape)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            AsyncImage(
                model = R.drawable.ic_discord,
                contentDescription = "Discord",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(15.dp, 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surface)
                    .padding(6.dp)
                    .size(24.dp)
            )
        }

        Column(
            modifier = Modifier
                .padding(15.dp, 5.dp, 15.dp, 15.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(surface)
        ) {
            Text(
                text = account?.displayName ?: "Discord",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = onSurface.copy(alpha = 0.9f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 12.dp, 20.dp, 0.dp)
            )

            Text(
                text = if (account != null) {
                    val base = if (account.discriminator != "0") "${account.username}#${account.discriminator}" else account.username
                    if (isSelected && customStatus != null) "$base \u2022 $customStatus" else base
                } else {
                    "Connect your Discord account"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = onSurface.copy(alpha = 0.9f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp, 4.dp)
            )

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(19.dp, 0.dp, 19.dp, 5.dp)
                    .height(1.5.dp)
                    .background(primary.copy(alpha = 0.3f))
            )

            if (account != null && isSelected && onLogoutClick != null) {
                Button(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 12.dp, 20.dp, 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Logout")
                }
            } else if (showLogin && onLoginClick != null) {
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 12.dp, 20.dp, 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Login")
                }
            }
        }
    }
}
