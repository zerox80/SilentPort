# SilentPort

SilentPort is an Android application that helps users manage their digital wellbeing by monitoring app usage patterns and providing intelligent privacy controls. The app automatically tracks which applications you use frequently and which ones you haven't opened in a while, offering both usage insights and network protection through an integrated firewall system.

## Why SilentPort?

Modern smartphones accumulate dozens of applications over time, many of which remain unused for extended periods while still consuming system resources and potentially accessing your network connection. SilentPort addresses this problem by:

- **Automated Usage Monitoring**: Continuously tracks your app usage patterns without requiring manual input
- **Smart categorization**: Automatically organizes apps into "Recently Used," "Rarely Used," and "Disabled" categories
- **Network Firewall**: Blocks network access for applications you haven't used recently, reducing potential privacy risks
- **Intelligent Notifications**: Sends timely reminders about apps that haven't been used in days
- **Usage Analytics**: Provides clear insights into your digital habits

## Core Features

### Usage Analysis
SilentPort monitors your app usage using Android's UsageStats API and categorizes applications based on their activity patterns:
- **Recently Used**: Apps opened within the recent threshold period
- **Rarely Used**: Apps not opened for an extended period but still active
- **Disabled**: Apps that have been automatically disabled due to prolonged inactivity

### Network Firewall
The integrated VPN-based firewall provides network protection by:
- Automatically blocking network access for rarely used applications
- Configurable allow duration (1 hour to 4 days) before blocking inactive apps
- Manual override controls for specific applications
- Real-time traffic monitoring capabilities for debugging and verification

### Smart Notifications
SilentPort sends proactive notifications to help you maintain app hygiene:
- Warning notifications when apps haven't been used for several days
- Recommendations to disable or uninstall applications you no longer need
- Clear integration with Android's app settings for easy management

### Usage Metrics
Optional metrics collection provides detailed insights:
- Network traffic analysis for rarely used applications
- Last 10 minutes of activity monitoring
- Manual refresh capabilities for real-time data

## Technical Architecture

SilentPort is built with modern Android development practices using:
- **Kotlin**: Primary programming language for type-safe, concise code
- **Jetpack Compose**: Modern UI toolkit for responsive, declarative interfaces
- **Room Database**: Local storage for persistent app usage tracking
- **WorkManager**: Background synchronization and periodic usage analysis
- **Android VPN Service**: Network-level traffic control and firewall functionality
- **Coroutines**: Asynchronous programming for responsive user experience

The application follows clean architecture principles with clear separation between:
- **Domain Layer**: Business logic for usage analysis and policy enforcement
- **Data Layer**: Local database storage and remote data synchronization
- **UI Layer**: Compose-based user interface with MVVM architecture
- **Infrastructure**: Network services, notification handling, and system integration

## Privacy and Security

SilentPort is designed with user privacy as the highest priority:
- All usage analysis happens locally on your device
- No usage data is transmitted to external servers
- Network monitoring only filters traffic without inspecting content
- Minimal permissions required (Usage Access and VPN for firewall functionality)
- Open-source architecture for complete transparency

## Getting Started

### Requirements
- Android 8.0 (API level 26) or higher
- Usage Access permission for app monitoring
- VPN permission (for firewall functionality)

### Installation
1. Download and install the SilentPort APK
2. Grant Usage Access permission when prompted
3. Enable firewall features by granting VPN permission
4. Configure your preferred allow duration and notification settings

### Initial Setup
The app automatically begins monitoring your application usage after permissions are granted. The first scan may take a few minutes as SilentPort builds your usage profile. Subsequent updates run automatically every 6 hours in the background.

## Configuration

### Firewall Settings
- **Allow Duration**: Configure how long apps can maintain network access after last use (1 hour, 6 hours, 1 day, or 4 days)
- **Manual Mode**: Require manual unblocking for apps that have been restricted
- **Metrics Collection**: Enable detailed network monitoring for troubleshooting

### Usage Policies
The app applies configurable thresholds for usage classification:
- Warning notifications after configurable periods of inactivity
- Automatic blocking recommendations after extended non-use
- Customizable time periods based on your usage patterns

## Contributing

SilentPort is an open-source project. We welcome contributions that:
- Improve the user experience and interface design
- Enhance privacy protection capabilities
- Optimize performance and battery efficiency
- Add new features while maintaining the app's privacy-first approach

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For bug reports, feature requests, or general questions about SilentPort, please refer to the project's issue tracker or documentation.

---

SilentPort helps you maintain control over your digital life by ensuring that only the applications you actually use have access to your device's resources and network connections. By automating the management of unused applications, it reduces digital clutter while enhancing your privacy and security.