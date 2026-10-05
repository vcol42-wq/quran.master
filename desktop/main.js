const { app, BrowserWindow, ipcMain, Notification, Tray, Menu, session, shell } = require('electron');
const path = require('path');

app.commandLine.appendSwitch('enable-speech-dispatcher');
app.commandLine.appendSwitch('use-fake-ui-for-media-stream');

let mainWindow = null;
let tray = null;
let isMiniMode = false;
let savedBounds = null;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1080,
    height: 720,
    center: true,
    minWidth: 360,
    minHeight: 400,
    title: 'سبح بخشوع',
    icon: path.join(__dirname, 'assets/icons/Square150x150Logo.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
      webSecurity: false
    },
    autoHideMenuBar: true,
    backgroundColor: '#064e3b',
    show: false
  });

  // Grant microphone and speech permissions unconditionally
  session.defaultSession.setPermissionRequestHandler((webContents, permission, callback) => {
    callback(true);
  });
  session.defaultSession.setPermissionCheckHandler(() => true);

  mainWindow.loadFile(path.join(__dirname, 'src/index.html'));

  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

function setupTray() {
  try {
    const iconPath = path.join(__dirname, 'assets/icons/Square44x44Logo.png');
    tray = new Tray(iconPath);
    const contextMenu = Menu.buildFromTemplate([
      {
        label: 'فتح سبح بخشوع',
        click: () => {
          if (mainWindow) {
            mainWindow.show();
            mainWindow.focus();
          }
        }
      },
      {
        label: 'السبحة المصغرة (فوق النوافذ)',
        click: () => {
          if (mainWindow) {
            mainWindow.show();
            mainWindow.focus();
            toggleMiniMode(true);
          }
        }
      },
      { type: 'separator' },
      {
        label: 'إغلاق التطبيق',
        click: () => {
          app.quit();
        }
      }
    ]);
    tray.setToolTip('سبح بخشوع - المصحف الشريف والأذكار');
    tray.setContextMenu(contextMenu);
    tray.on('double-click', () => {
      if (mainWindow) {
        mainWindow.show();
        mainWindow.focus();
      }
    });
  } catch (e) {
    console.error('Tray setup failed:', e);
  }
}

function toggleMiniMode(enable) {
  if (!mainWindow) return;
  if (enable && !isMiniMode) {
    savedBounds = mainWindow.getBounds();
    isMiniMode = true;
    mainWindow.setAlwaysOnTop(true, 'floating');
    mainWindow.setSize(380, 520, true);
    mainWindow.webContents.send('mini-mode-changed', true);
  } else if (!enable && isMiniMode) {
    isMiniMode = false;
    mainWindow.setAlwaysOnTop(false);
    if (savedBounds) {
      mainWindow.setBounds(savedBounds, true);
    } else {
      mainWindow.setSize(1200, 820, true);
    }
    mainWindow.webContents.send('mini-mode-changed', false);
  }
}

ipcMain.on('toggle-mini-mode', (event, enable) => {
  toggleMiniMode(enable);
});

ipcMain.on('window-minimize', () => {
  if (mainWindow) mainWindow.minimize();
});

ipcMain.on('window-maximize', () => {
  if (mainWindow) {
    if (mainWindow.isMaximized()) {
      mainWindow.unmaximize();
    } else {
      mainWindow.maximize();
    }
  }
});

ipcMain.on('window-close', () => {
  if (mainWindow) mainWindow.close();
});

ipcMain.on('notify', (event, { title, body }) => {
  if (Notification.isSupported()) {
    new Notification({
      title: title || 'سبح بخشوع',
      body: body || '',
      icon: path.join(__dirname, 'assets/icons/Square150x150Logo.png')
    }).show();
  }
});

ipcMain.on('open-external', (event, url) => {
  if (url && (url.startsWith('https://') || url.startsWith('http://'))) {
    shell.openExternal(url);
  }
});

app.whenReady().then(() => {
  const { session } = require('electron');
  if (session && session.defaultSession) {
    session.defaultSession.setPermissionRequestHandler((webContents, permission, callback) => {
      if (permission === 'media') {
        return callback(true);
      }
      callback(true);
    });
  }

  createWindow();
  setupTray();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
