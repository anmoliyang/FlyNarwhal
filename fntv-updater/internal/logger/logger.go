package logger

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"

	"fntv_updater/internal/consts"
	"fntv_updater/internal/ui"

	"github.com/fatih/color"
)

var LogFile *os.File

func InitLog() {
	logDir := "logs"
	if _, err := os.Stat(logDir); os.IsNotExist(err) {
		os.Mkdir(logDir, 0755)
	}

	cleanOldLogs(logDir)

	date := time.Now().Format("2006-01-02")
	logFilePath := filepath.Join(logDir, fmt.Sprintf("updater-%s.log", date))

	f, err := os.OpenFile(logFilePath, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		fmt.Println("Failed to open log file:", err)
		return
	}
	LogFile = f
}

func cleanOldLogs(logDir string) {
	files, err := os.ReadDir(logDir)
	if err != nil {
		return
	}

	today := time.Now()
	// Keep today, yesterday, day before yesterday.
	// So delete if difference >= 3 days
	retentionDays := 3.0

	for _, file := range files {
		if file.IsDir() {
			continue
		}
		name := file.Name()
		if strings.HasPrefix(name, "updater-") && strings.HasSuffix(name, ".log") {
			datePart := strings.TrimSuffix(strings.TrimPrefix(name, "updater-"), ".log")
			fileDate, err := time.Parse("2006-01-02", datePart)
			if err != nil {
				continue
			}

			// Calculate days difference
			// We use Truncate to compare dates only, ignoring time
			d1 := time.Date(today.Year(), today.Month(), today.Day(), 0, 0, 0, 0, today.Location())
			d2 := time.Date(fileDate.Year(), fileDate.Month(), fileDate.Day(), 0, 0, 0, 0, fileDate.Location())

			days := d1.Sub(d2).Hours() / 24

			if days >= retentionDays {
				err := os.Remove(filepath.Join(logDir, name))
				if err != nil {
					fmt.Println("Failed to delete old log:", name, err)
				} else {
					fmt.Println("Deleted old log:", name)
				}
			}
		}
	}
}

// Logging
func logMsg(level string, c *color.Color, format string, args ...interface{}) {
	timestamp := time.Now().Format("2006-01-02 15:04:05")
	msg := fmt.Sprintf(format, args...)
	levelStr := fmt.Sprintf("[%s]", strings.ToUpper(level))

	// Print to console
	fmt.Printf("[%s] %s\n", timestamp, c.Sprint(levelStr+" "+msg))

	// Write to file
	if LogFile != nil {
		fullMsg := fmt.Sprintf("[%s] %s %s\n", timestamp, levelStr, msg)
		LogFile.WriteString(fullMsg)
	}
}

func Info(format string, args ...interface{}) {
	logMsg("info", color.New(color.FgHiCyan), format, args...)
}

func Success(format string, args ...interface{}) {
	logMsg("success", color.New(color.FgHiGreen), format, args...)
}

func Warn(format string, args ...interface{}) {
	logMsg("warn", color.New(color.FgHiYellow), format, args...)
	ui.ShowMessageBox(fmt.Sprintf(format, args...), "FnMedia Updater - Warning", consts.MB_OK|consts.MB_ICONWARNING|consts.MB_SYSTEMMODAL)
}

func ErrorLog(format string, args ...interface{}) {
	logMsg("error", color.New(color.FgHiRed), format, args...)
	ui.ShowMessageBox(fmt.Sprintf(format, args...), "FnMedia Updater - Error", consts.MB_OK|consts.MB_ICONERROR|consts.MB_SYSTEMMODAL)
}
