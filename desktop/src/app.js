// =========================================================================
// سبح بخشوع - Desktop App Engine (طِبق الأصل من تطبيق أندرويد)
// =========================================================================

// --- 1. Realistic Acoustic Bead Audio Synthesizer ---
class BeadAudioEngine {
  constructor() {
    this.ctx = null;
    this.enabled = true;
  }

  init() {
    if (!this.ctx) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (AudioCtx) this.ctx = new AudioCtx();
    }
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  playBeadClick() {
    if (!this.enabled) return;
    try {
      this.init();
      if (!this.ctx) return;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = 'sine';
      const now = this.ctx.currentTime;
      osc.frequency.setValueAtTime(820, now);
      osc.frequency.exponentialRampToValueAtTime(140, now + 0.04);

      gain.gain.setValueAtTime(0.35, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.04);

      osc.connect(gain);
      gain.connect(this.ctx.destination);

      osc.start(now);
      osc.stop(now + 0.045);
    } catch (e) {
      console.warn('Audio click error:', e);
    }
  }

  playGoalChime() {
    if (!this.enabled) return;
    try {
      this.init();
      if (!this.ctx) return;
      const now = this.ctx.currentTime;
      // Gentle spiritual chord (C - E - G - High C)
      const freqs = [523.25, 659.25, 783.99, 1046.50];
      freqs.forEach((freq, idx) => {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(freq, now + idx * 0.07);
        gain.gain.setValueAtTime(0.22, now + idx * 0.07);
        gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.07 + 0.55);
        osc.connect(gain);
        gain.connect(this.ctx.destination);
        osc.start(now + idx * 0.07);
        osc.stop(now + idx * 0.07 + 0.6);
      });
    } catch (e) {
      console.warn('Audio chime error:', e);
    }
  }
}

const audioEngine = new BeadAudioEngine();

// --- 2. Application State ---
const state = {
  activeTab: 'home',
  homeMode: 'cards', // 'cards' or 'beads'
  tasbeeh: {
    phrase: 'سُبْحَانَ اللَّهِ',
    virtue: 'أحب الكلام إلى الله تعالى',
    count: 0,
    target: 33,
    type: 'circle', // 'circle' (33) or 'spiral' (99)
    animOffset: 0
  },
  quran: {
    currentSurahId: 1,
    currentPageNum: 1,
    currentJuzNum: 1,
    currentManzilNum: 1,
    totalRukoo: 0,
    sessionRukoo: 0,
    sessionTimerSeconds: 0,
    sessionTimerInterval: null,
    reviewMode: false,
    playingAudio: null,
    currentPlaying: null,
    continuousAudio: localStorage.getItem('sb_quran_continuous_audio') !== 'false',
    hifzRepeatCount: parseInt(localStorage.getItem('sb_hifz_repeat') || '1', 10),
    hifzCurrentIteration: 1,
    translationLang: localStorage.getItem('sb_quran_trans_lang') || 'none',
    reciterId: localStorage.getItem('sb_quran_reciter_id') || 'Alafasy_128kbps',
    isRestoringProgress: false,
    wordHidingActive: false,
    speechTrackerActive: false,
    speechRecognition: null,
    selectedVerse: null,
    fontSize: 26,
    fontFamily: 'AmiriQuran',
    theme: 'parchment'
  },
  azkar: {},
  hadiths: [],
  prayer: {
    nextName: 'الظهر',
    countdown: ''
  },
  isMiniMode: false
};

// --- 3. DOM Ready Initialization ---
document.addEventListener('DOMContentLoaded', async () => {
  if (typeof initThemeSystem === 'function') initThemeSystem();
  initNebulaSplash();
  setupNavigation();
  setupSidebarToggle();
  setupHomeModes();
  setupTutorialVideoEngine();
  setupTasbeehEngine();
  setupQuranRoll();
  setupPrayerEngine();
  setupIslamicAIAssistant();
  setupPrivacyPolicy();

  // Load Secondary Data
  await loadAzkarData();
  await loadHadithData();

  // Sound toggle
  const soundBtn = document.getElementById('btn-sound-toggle');
  if (soundBtn) {
    soundBtn.addEventListener('click', () => {
      audioEngine.enabled = !audioEngine.enabled;
      soundBtn.style.opacity = audioEngine.enabled ? '1' : '0.4';
    });
  }

  // Mini mode
  const miniBtn = document.getElementById('btn-mini-mode');
  if (miniBtn) {
    miniBtn.addEventListener('click', () => {
      state.isMiniMode = !state.isMiniMode;
      document.body.classList.toggle('mini-mode', state.isMiniMode);
      if (window.electronAPI && window.electronAPI.toggleMiniMode) {
        window.electronAPI.toggleMiniMode(state.isMiniMode);
      }
    });
  }

  updateDateDisplay();
});

// --- 3.1 Symmetrical Sidebar & Fullscreen Manager (ثلاث خطوط متوازية لإظهار / إخفاء القائمة والأهداف) ---
function setupSidebarToggle() {
  const appContainer = document.querySelector('.app-container');
  const btnGlobal = document.getElementById('btnGlobalSidebarToggle');
  const btnCollapse = document.getElementById('btnSidebarCollapse');
  const btnQuran = document.getElementById('btnSidebarExpandQuran');
  const btnTasbeeh = document.getElementById('btnSidebarExpandTasbeeh');
  const btnTasbeehFocus = document.getElementById('btnToggleTasbeehFocus');
  const labelTasbeehFocus = document.getElementById('labelTasbeehFocus');

  let isCollapsed = localStorage.getItem('sb_sidebar_collapsed') === 'true';

  function applySidebarState() {
    if (!appContainer) return;
    appContainer.classList.toggle('sidebar-collapsed', isCollapsed);
    try {
      localStorage.setItem('sb_sidebar_collapsed', isCollapsed);
    } catch (e) {}

    const tipText = isCollapsed ? 'إظهار القائمة الجانبية والأهداف (☰)' : 'إخفاء القائمة الجانبية (شاشة كاملة)';
    if (btnGlobal) btnGlobal.title = tipText;
    if (btnCollapse) btnCollapse.title = tipText;
    if (btnQuran) btnQuran.title = tipText;
    if (btnTasbeeh) btnTasbeeh.title = tipText;

    if (window.renderTasbeehEngine) {
      setTimeout(window.renderTasbeehEngine, 320);
    }
  }

  function toggleSidebar() {
    isCollapsed = !isCollapsed;
    applySidebarState();
  }

  if (btnGlobal) btnGlobal.addEventListener('click', toggleSidebar);
  if (btnCollapse) btnCollapse.addEventListener('click', toggleSidebar);
  if (btnQuran) btnQuran.addEventListener('click', toggleSidebar);
  if (btnTasbeeh) btnTasbeeh.addEventListener('click', toggleSidebar);

  // Keyboard shortcut: Alt+M to toggle sidebar
  window.addEventListener('keydown', (e) => {
    if (e.altKey && (e.code === 'KeyM' || e.key === 'm' || e.key === 'م')) {
      e.preventDefault();
      toggleSidebar();
    }
  });

  // Pure Shrine Focus Mode for Misbaha (إخفاء / إظهار قوائم التسبيحات الجانبية داخل شاشة المسبحة)
  if (btnTasbeehFocus) {
    let isFocusOnly = localStorage.getItem('sb_tasbeeh_focus_only') === 'true';

    function applyFocusState() {
      const grid = document.querySelector('.tasbeeh-sanctuary-grid');
      if (grid) grid.classList.toggle('focus-shrine-only', isFocusOnly);
      if (labelTasbeehFocus) {
        labelTasbeehFocus.textContent = isFocusOnly ? 'إظهار الأهداف' : 'ملء الشاشة';
      }
      try {
        localStorage.setItem('sb_tasbeeh_focus_only', isFocusOnly);
      } catch (e) {}

      if (window.renderTasbeehEngine) {
        setTimeout(window.renderTasbeehEngine, 200);
      }
    }

    btnTasbeehFocus.addEventListener('click', () => {
      isFocusOnly = !isFocusOnly;
      applyFocusState();
    });

    applyFocusState();
  }

  applySidebarState();
}

function updateDateDisplay() {
  const el = document.getElementById('current-date-display');
  if (el) {
    el.textContent = new Date().toLocaleDateString('ar-SA', {
      weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
    });
  }
}

// --- 4. Nebula Starry Splash Screen (مطابقة لـ NebulaView.kt) ---
function initNebulaSplash() {
  const canvas = document.getElementById('nebula-canvas');
  const splash = document.getElementById('splash-screen');
  if (!canvas || !splash) return;

  if (window.location.hash) {
    splash.remove();
    return;
  }

  const ctx = canvas.getContext('2d');
  let width = canvas.width = window.innerWidth;
  let height = canvas.height = window.innerHeight;

  window.addEventListener('resize', () => {
    width = canvas.width = window.innerWidth;
    height = canvas.height = window.innerHeight;
  });

  const colors = ['#87CEEB', '#E6E6FA', '#FFB6C1', '#FFFFFF', '#66CDAA'];
  const particles = [];
  for (let i = 0; i < 90; i++) {
    particles.push({
      x: Math.random() * width,
      y: Math.random() * height,
      r: Math.random() * 3 + 1,
      alpha: Math.random(),
      dx: (Math.random() - 0.5) * 1.2,
      dy: (Math.random() - 0.5) * 1.2,
      color: colors[Math.floor(Math.random() * colors.length)]
    });
  }

  let animationFrame;
  function render() {
    ctx.clearRect(0, 0, width, height);
    particles.forEach(p => {
      p.x += p.dx;
      p.y += p.dy;
      if (p.x < 0) p.x = width;
      if (p.x > width) p.x = 0;
      if (p.y < 0) p.y = height;
      if (p.y > height) p.y = 0;

      ctx.save();
      ctx.globalAlpha = p.alpha;
      ctx.fillStyle = p.color;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    });
    animationFrame = requestAnimationFrame(render);
  }
  render();

  function dismissSplash() {
    splash.classList.add('fade-out');
    setTimeout(() => {
      cancelAnimationFrame(animationFrame);
      splash.remove();
    }, 850);
  }

  // Dismiss after 2.4 seconds or on click
  setTimeout(dismissSplash, 2400);
  splash.addEventListener('click', dismissSplash);
}

// --- 5. Navigation ---
function setupNavigation() {
  const navBtns = document.querySelectorAll('.nav-item');
  const panels = document.querySelectorAll('.view-panel');
  const viewTitle = document.getElementById('view-title');

  const titles = {
    home: 'الرئيسية',
    tasbeeh: 'السبحة الذكية',
    quran: 'المصحف الشريف',
    azkar: 'موسوعة الأذكار والأوراد',
    hadith: 'الأحاديث النبوية الشريفة',
    prayer: 'مواقيت الصلاة والأذان',
    assistant: 'مساعد التدبر الذكي',
    settings: 'الإعدادات'
  };

  window.switchTab = function(tabName, skipAutoRestore = false) {
    state.activeTab = tabName;
    const mainContent = document.querySelector('.main-content');
    if (mainContent) mainContent.scrollTop = 0;
    navBtns.forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-tab') === tabName);
    });
    panels.forEach(p => p.classList.remove('active'));
    const targetPanel = document.getElementById(`panel-${tabName}`);
    if (targetPanel) {
      targetPanel.classList.add('active');
      if (tabName !== 'azkar' && tabName !== 'quran' && tabName !== 'hadith') {
        targetPanel.scrollTop = 0;
      }
    }
    if (viewTitle) viewTitle.textContent = titles[tabName] || 'سبح بخشوع';

    // If Tasbeeh tab is opened, render and update canvas immediately
    if (tabName === 'tasbeeh' && window.renderTasbeehEngine) {
      setTimeout(() => {
        window.renderTasbeehEngine();
      }, 30);
    }

    // If Azkar tab is opened, refresh Morning/Evening Hero UI state and restore scroll/progress
    if (tabName === 'azkar') {
      if (typeof updateMorningEveningHeroUI === 'function') {
        updateMorningEveningHeroUI();
      }
      if (typeof restoreAzkarProgressState === 'function') {
        setTimeout(() => restoreAzkarProgressState(), 60);
      }
    }

    // If Quran tab is opened, restore saved page progress only if not skipping
    if (tabName === 'quran' && !skipAutoRestore) {
      const savedPage = Math.max(1, Math.min(604, parseInt(localStorage.getItem('sb_last_page_num') || '1', 10)));
      const savedVerse = parseInt(localStorage.getItem('sb_last_verse_num') || '1', 10);
      state.quran.isRestoringProgress = true;
      setTimeout(() => {
        if (typeof jumpToQuranPage === 'function') {
          jumpToQuranPage(savedPage, savedVerse > 0 ? savedVerse : null, false);
        }
        setTimeout(() => {
          state.quran.isRestoringProgress = false;
        }, 800);
      }, 50);
    }
  };

  navBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      switchTab(btn.getAttribute('data-tab'));
    });
  });

  // Sidebar Unified Search Button
  const sidebarBtnSearch = document.getElementById('sidebar-btn-search');
  if (sidebarBtnSearch) {
    sidebarBtnSearch.addEventListener('click', () => {
      if (typeof window.openGlobalSearchModal === 'function') {
        window.openGlobalSearchModal();
      }
    });
  }

  // Global Keyboard Shortcut: Ctrl+F / Ctrl+K for Unified Search
  window.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && (e.key === 'f' || e.key === 'k' || e.key === 'F' || e.key === 'K')) {
      e.preventDefault();
      if (typeof window.openGlobalSearchModal === 'function') {
        window.openGlobalSearchModal();
      }
    }
  });

  const brandHome = document.getElementById('btn-brand-home');
  if (brandHome) brandHome.addEventListener('click', () => switchTab('home'));

  // Action cards on home screen that navigate to tabs
  document.querySelectorAll('[data-target-tab]').forEach(el => {
    el.addEventListener('click', () => {
      const tab = el.getAttribute('data-target-tab');
      if (tab) switchTab(tab);
    });
  });

  // Direct tab activation via hash (e.g. #tasbeeh, #azkar)
  const initialHash = (window.location.hash || '').replace('#', '');
  if (initialHash && titles[initialHash]) {
    switchTab(initialHash);
  }
}

// --- 6. Dual Home Screen (البطاقات والواجهة التفاعلية طِبق الأصل من أندرويد) ---
function setupHomeModes() {
  const btnSwitchExp = document.getElementById('btnSwitchToExpHome');
  const btnSwitchStd = document.getElementById('btnSwitchToStandardHome');
  const btnScrollTop = document.getElementById('btnScrollToTopCard');
  const cardsView = document.getElementById('home-cards-view');
  const beadsView = document.getElementById('home-beads-view');
  const panelHome = document.getElementById('panel-home');

  function setMode(mode) {
    state.homeMode = mode;
    localStorage.setItem('sb_home_mode', mode);
    if (mode === 'beads') {
      if (cardsView) cardsView.style.display = 'none';
      if (beadsView) beadsView.style.display = 'flex';
    } else {
      if (cardsView) cardsView.style.display = 'flex';
      if (beadsView) beadsView.style.display = 'none';
    }
    if (panelHome) panelHome.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // Restore saved home mode
  const savedMode = localStorage.getItem('sb_home_mode') || 'cards';
  setMode(savedMode);

  if (btnSwitchExp) {
    btnSwitchExp.addEventListener('click', () => setMode('beads'));
  }
  if (btnSwitchStd) {
    btnSwitchStd.addEventListener('click', () => setMode('cards'));
  }
  if (btnScrollTop) {
    btnScrollTop.addEventListener('click', () => {
      if (panelHome) panelHome.scrollTo({ top: 0, behavior: 'smooth' });
    });
  }

  const footerScrollTop = document.getElementById('cardFooterScrollToTop');
  if (footerScrollTop) {
    footerScrollTop.addEventListener('click', () => {
      if (panelHome) panelHome.scrollTo({ top: 0, behavior: 'smooth' });
    });
  }

  const cardArticleGhg = document.getElementById('cardFooterGhg');
  if (cardArticleGhg) {
    cardArticleGhg.addEventListener('click', () => {
      const assistantInput = document.getElementById('assistant-input');
      if (assistantInput) {
        assistantInput.value = 'ما معنى التسبيح لغة وشرعاً، وما هي فضائله العظيمة في القرآن والسنة؟';
      }
    });
  }

  // Cards and Beads Navigation handlers
  document.querySelectorAll('#panel-home [data-target-tab]').forEach(el => {
    el.addEventListener('click', (e) => {
      // Audio click acoustic feedback
      if (typeof audioEngine !== 'undefined' && audioEngine.playBeadClick) {
        audioEngine.playBeadClick();
      }

      const targetTab = el.getAttribute('data-target-tab');
      const phrase = el.getAttribute('data-phrase');
      const category = el.getAttribute('data-category');

      // Settings trigger
      if (targetTab === 'settings') {
        const fontModal = document.getElementById('modalFontSettings');
        if (fontModal) {
          fontModal.classList.add('open');
        } else {
          const btnFont = document.getElementById('btn-open-font-settings');
          if (btnFont) btnFont.click();
        }
        return;
      }

      // Azkar subcategory filtering trigger (e.g. دعاء المحزون)
      if (targetTab === 'azkar' && category) {
        if (typeof azkarState !== 'undefined') {
          azkarState.activeGroup = 'الكل';
          azkarState.activeSubcategory = category;
          const sel = document.getElementById('selectAzkarSubcategory');
          if (sel) sel.value = category;
          if (typeof filterAndRenderAzkar === 'function') filterAndRenderAzkar();
        }
      }

      // Quran dynamic resume trigger
      if (targetTab === 'quran' && (el.id === 'cardDynamicHeader' || el.id === 'panelTopHeader' || el.classList.contains('dynamic-header-card') || el.classList.contains('quran-flow-header-card'))) {
        const savedPage = Math.max(1, Math.min(604, parseInt(localStorage.getItem('sb_last_page_num') || '1', 10)));
        const savedVerse = parseInt(localStorage.getItem('sb_last_verse_num') || '1', 10);
        if (typeof switchTab === 'function') switchTab('quran');
        if (typeof jumpToQuranPage === 'function') {
          setTimeout(() => jumpToQuranPage(savedPage, savedVerse > 1 ? savedVerse : null, false), 100);
        }
        return;
      }

      // Tasbeeh target setup
      if (phrase && targetTab === 'tasbeeh') {
        state.tasbeeh.phrase = phrase;
        state.tasbeeh.count = 0;
        if (phrase.includes('صَلِّ عَلَى مُحَمَّدٍ')) {
          state.tasbeeh.virtue = 'من صلى علي صلاة صلى الله عليه بها عشراً';
          state.tasbeeh.target = 100;
        } else if (phrase.includes('أَسْتَغْفِرُ')) {
          state.tasbeeh.virtue = 'الاستغفار يمحو الذنوب ويفتح الأرزاق ويدفع البلاء';
          state.tasbeeh.target = 100;
        }
        if (window.renderTasbeehEngine) window.renderTasbeehEngine();
      }

      if (targetTab && window.switchTab) {
        window.switchTab(targetTab);
      }
    });
  });

  // Small Separator Beads Audio Clicks and Bounce
  document.querySelectorAll('.android-small-separator-bead, #topConnectedBead, .tassel-head-bead').forEach(bead => {
    bead.addEventListener('click', (e) => {
      e.stopPropagation();
      if (typeof audioEngine !== 'undefined' && audioEngine.playBeadClick) {
        audioEngine.playBeadClick();
      }
      bead.style.transform = 'scale(1.22)';
      setTimeout(() => { bead.style.transform = ''; }, 160);
    });
  });

  // Floating Bottom Navigation Bar (layout_bottom_bar.xml)
  const homeBottomBar = document.getElementById('homeBottomBar');
  const btnHomeBbHome = document.getElementById('home-bb-btn-home');
  const btnHomeBbSearch = document.getElementById('home-bb-btn-search');
  const btnHomeBbAssistant = document.getElementById('home-bb-btn-assistant');
  const btnHomeBbMushaf = document.getElementById('home-bb-btn-mushaf');
  const btnHomeBbSettings = document.getElementById('home-bb-btn-settings');

  if (btnHomeBbHome) {
    btnHomeBbHome.addEventListener('click', () => {
      if (typeof audioEngine !== 'undefined' && audioEngine.playBeadClick) audioEngine.playBeadClick();
      if (panelHome) panelHome.scrollTo({ top: 0, behavior: 'smooth' });
    });
  }

  if (btnHomeBbSearch) {
    btnHomeBbSearch.addEventListener('click', () => {
      if (typeof audioEngine !== 'undefined' && audioEngine.playBeadClick) audioEngine.playBeadClick();
      const modal = document.getElementById('modalSearch');
      if (modal) {
        modal.classList.add('open');
        const input = document.getElementById('inputQuranSearch');
        if (input) setTimeout(() => input.focus(), 150);
      }
    });
  }

  if (btnHomeBbAssistant) {
    btnHomeBbAssistant.addEventListener('click', () => {
      if (typeof audioEngine !== 'undefined' && audioEngine.playBeadClick) audioEngine.playBeadClick();
      if (window.switchTab) window.switchTab('assistant');
    });
  }

  if (btnHomeBbMushaf) {
    btnHomeBbMushaf.addEventListener('click', () => {
      if (typeof audioEngine !== 'undefined' && audioEngine.playBeadClick) audioEngine.playBeadClick();
      if (window.switchTab) window.switchTab('quran');
    });
  }

  if (btnHomeBbSettings) {
    btnHomeBbSettings.addEventListener('click', () => {
      if (typeof audioEngine !== 'undefined' && audioEngine.playBeadClick) audioEngine.playBeadClick();
      const fontModal = document.getElementById('modalFontSettings');
      if (fontModal) fontModal.classList.add('open');
      else {
        const btnFont = document.getElementById('btn-open-font-settings');
        if (btnFont) btnFont.click();
      }
    });
  }

  // Smooth scroll listener for bottom bar auto-hide/reveal
  if (panelHome && homeBottomBar) {
    let lastScrollY = panelHome.scrollTop;
    panelHome.addEventListener('scroll', () => {
      const currentScrollY = panelHome.scrollTop;
      if (currentScrollY > 120 && currentScrollY > lastScrollY + 15) {
        // Scrolling down -> hide bottom bar
        homeBottomBar.classList.add('bottom-bar-hidden');
      } else if (currentScrollY < lastScrollY - 10 || currentScrollY <= 60) {
        // Scrolling up or near top -> reveal bottom bar
        homeBottomBar.classList.remove('bottom-bar-hidden');
      }
      lastScrollY = currentScrollY;
    }, { passive: true });
  }

  // Open External Links (Google Play Store & Website links)
  document.querySelectorAll('[data-external-url]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const url = btn.getAttribute('data-external-url');
      if (url) {
        if (window.electronAPI && window.electronAPI.openExternal) {
          window.electronAPI.openExternal(url);
        } else {
          window.open(url, '_blank');
        }
      }
    });
  });

  // Wire Tutorial Video card & actions
  const cardTutorial = document.getElementById('cardTutorialVideo');
  const expActionTutorial = document.getElementById('expActionTutorialVideo');
  if (cardTutorial) {
    cardTutorial.addEventListener('click', () => {
      if (typeof openTutorialVideoModal === 'function') openTutorialVideoModal();
    });
  }
  if (expActionTutorial) {
    expActionTutorial.addEventListener('click', () => {
      if (typeof openTutorialVideoModal === 'function') openTutorialVideoModal();
    });
  }

  // Update Dynamic Flow Header Subtitle with saved Quran progress
  window.updateDynamicHeaderProgress = function() {
    const subtitleEl = document.getElementById('tvDynamicHeaderSubtitle');
    if (subtitleEl) {
      const savedSurah = localStorage.getItem('sb_last_surah_name');
      const savedPage = localStorage.getItem('sb_last_page_num');
      const savedVerse = localStorage.getItem('sb_last_verse_num');
      if (savedSurah && savedPage) {
        const verseStr = savedVerse ? ` (آية ${toArabicDigits(savedVerse)})` : '';
        subtitleEl.textContent = `سورة ${savedSurah}${verseStr} • صـ ${toArabicDigits(savedPage)} • انقر للمتابعة 🔖`;
      } else {
        subtitleEl.textContent = 'سورة البقرة • انقر للمتابعة 🔖';
      }
    }
  };
  window.updateDynamicHeaderProgress();
}

// =========================================================================
// Interactive Tutorial Video & Quick Guide Showcase Module
// جولة تعريفية تفاعلية وسلسة تشرح كل أقسام ومميزات التطبيق
// =========================================================================
// =========================================================================
// 6. Home Embedded Video Ad & Promotional Showcase Theatre Engine
// فيديو تعليمي وإعلاني شامل عالي الجودة مدمج مباشرة في الواجهة الرئيسية
// =========================================================================
function setupTutorialVideoEngine() {
  const canvas = document.getElementById('homeVideoAdCanvas');
  const videoEl = document.getElementById('homeVideoAdElement');
  const playOverlay = document.getElementById('homeVideoPlayOverlay');
  const playPauseBtn = document.getElementById('btnHomeVideoPlayPause');
  const iconPlay = document.getElementById('iconHomeVideoPlay');
  const iconPause = document.getElementById('iconHomeVideoPause');
  const muteBtn = document.getElementById('btnHomeVideoMute');
  const iconSoundOn = document.getElementById('iconHomeVideoSoundOn');
  const iconSoundOff = document.getElementById('iconHomeVideoSoundOff');
  const fullscreenBtn = document.getElementById('btnHomeVideoFullscreen');
  const stageContainer = document.getElementById('homeVideoStageContainer');
  const sceneTagEl = document.getElementById('homeVideoSceneTag');
  const sceneHeadlineEl = document.getElementById('homeVideoSceneHeadline');
  const timeDisplay = document.getElementById('homeVideoTimeDisplay');
  const timelineFill = document.getElementById('homeVideoTimelineFill');
  const timelineWrap = document.getElementById('homeVideoTimelineWrap');
  const chapterButtons = document.querySelectorAll('#homeVideoChaptersStrip .btn-video-chapter');
  const shareBtn = document.getElementById('btnShareVideoAd');

  if (!canvas) return;
  const ctx = canvas.getContext('2d');

  // Preload App Logo for Outro and watermark
  const appLogoImg = new Image();
  appLogoImg.src = '../assets/images/nnbb.png';

  // Helper: Bulletproof rounded rectangle for canvas
  function drawRoundRect(c, x, y, w, h, r) {
    if (typeof r === 'undefined') r = 8;
    if (typeof r === 'number') r = [r, r, r, r];
    const [tl, tr, br, bl] = Array.isArray(r) ? (r.length === 1 ? [r[0], r[0], r[0], r[0]] : r) : [r, r, r, r];
    c.beginPath();
    c.moveTo(x + tl, y);
    c.lineTo(x + w - tr, y);
    c.quadraticCurveTo(x + w, y, x + w, y + tr);
    c.lineTo(x + w, y + h - br);
    c.quadraticCurveTo(x + w, y + h, x + w - br, y + h);
    c.lineTo(x + bl, y + h);
    c.quadraticCurveTo(x, y + h, x, y + h - bl);
    c.lineTo(x, y + tl);
    c.quadraticCurveTo(x, y, x + tl, y);
    c.closePath();
  }

  const chapters = [
    {
      id: 'quran',
      tag: '📖 المصحف الشريف والتلاوات الخاشعة',
      headline: 'قراءة ميسرة بالرسم العثماني المعتمد وتلاوات لكبار القراء مع التفسير والمحفظ الآلي',
      colorStart: '#0E442D',
      colorEnd: '#1A6343',
      accentColor: '#D4AF37',
      speechText: 'المصحف الشريف في تطبيق سبح بخشوع: قراءة سلسة لكامل صفحات القرآن الكريم بالرسم العثماني المعتمد، مع تلاوات خاشعة، وتفسير ميسر، ومحفظ آلي لتكرار الآيات وتثبيت الحفظ.'
    },
    {
      id: 'tasbeeh',
      tag: '📿 السبحة الإلكترونية الذكية ثلاثية الأبعاد',
      headline: 'محاكاة فيزيائية واقعية للخرز الطبيعي مع أصوات نقر مريحة وأهداف استغفار وتسبيح يومية',
      colorStart: '#144B32',
      colorEnd: '#22704B',
      accentColor: '#F59E0B',
      speechText: 'السبحة الذكية: سبحة إلكترونية تفاعلية تحاكي الخرز الطبيعي بنمط الثلاثة والثلاثين والمائة، بأصوات نقر واقعية، وأهداف مخصصة، والسبحة المصغرة للعمل فوق البرامج.'
    },
    {
      id: 'azkar',
      tag: '🌅 موسوعة الأذكار والأوراد النبوية الشاملة',
      headline: 'حصن المسلم و١٤٩ باباً مصنفاً من صحيح السنة مع بطاقة الصباح والمساء التلقائية',
      colorStart: '#165337',
      colorEnd: '#277A50',
      accentColor: '#10B981',
      speechText: 'موسوعة الأذكار: حصن المسلم الشامل مع بطاقة أذكار الصباح والمساء التلقائية بحسب وقتك، وعداد ذكي تفاعلي لكل ذكر، وتخريج الأحاديث من صحيح السنة النبوية.'
    },
    {
      id: 'hadith',
      tag: '📜 موسوعة الأحاديث النبوية والشروح السلفية',
      headline: 'أكثر من ٥٠,٠٠٠ حديث محقق من الصحيحين والسنن والأربعينيات مع البحث الفوري الشامل',
      colorStart: '#12462F',
      colorEnd: '#1E6847',
      accentColor: '#60A5FA',
      speechText: 'الأحاديث النبوية: مكتبة إسلامية ضخمة تضم سبعة عشر كتاباً معتمداً وأكثر من خمسين ألف حديث شريف مع الشروح المعتمدة والبحث الفوري الفائق.'
    },
    {
      id: 'prayer',
      tag: '🕌 مواقيت الصلاة الدقيقة وبوصلة القبلة المشرفة',
      headline: 'حساب فلكي دقيق لجميع مدن العالم مع تنبيهات الأذان والعد التنازلي وبوصلة القبلة',
      colorStart: '#0F442E',
      colorEnd: '#1B6444',
      accentColor: '#FBBF24',
      speechText: 'مواقيت الصلاة والقبلة: حساب دقيق لمواقيت الصلاة بحسب موقعك مع تنبيهات الأذان، وبوصلة ثلاثية الأبعاد تدلك على اتجاه الكعبة المشرفة في أي مكان بالعالم.'
    },
    {
      id: 'outro',
      tag: '🌟 سبّح بخشوع - رفيقك اليومي إلى الجنة',
      headline: 'تطبيق مجاني بالكامل بدون إعلانات مزعجة - متوفر الآن على متجر Google Play الرسمي',
      colorStart: '#114831',
      colorEnd: '#206B49',
      accentColor: '#34D399',
      speechText: 'احصل على تطبيق سبح بخشوع الآن مجاناً على هاتفك الأندرويد عبر متجر جوجل بلاي، وشاركه مع أحبابك، فالدال على الخير كفاعله.'
    }
  ];

  let currentChapter = 0;
  let isPlaying = false;
  let isMuted = false;
  let chapterTimerSeconds = 0;
  const CHAPTER_DURATION = 15; // 15 seconds per chapter (90 seconds total)
  let timerInterval = null;
  let animationFrameId = null;
  let animTick = 0;

  // 1. Draw Visual Frame on Canvas (Ultra-Sharp 1920x1080 Full HD Engine)
  function drawFrame() {
    animTick += 0.035;
    const w = 1920;
    const h = 1080;
    const ch = chapters[currentChapter];

    // Background Radiant Emerald Gradient (No dark black shadows)
    const bgGrad = ctx.createLinearGradient(0, 0, w, h);
    bgGrad.addColorStop(0, ch.colorStart);
    bgGrad.addColorStop(0.5, ch.colorEnd);
    bgGrad.addColorStop(1, '#082E1E');
    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, w, h);

    // Decorative Islamic Geometric Stars & Particles
    ctx.save();
    ctx.strokeStyle = 'rgba(212, 175, 55, 0.16)';
    ctx.lineWidth = 2.5;
    for (let i = 0; i < 8; i++) {
      const angle = (i * Math.PI / 4) + animTick * 0.08;
      const rx = w / 2 + Math.cos(angle) * 440;
      const ry = h / 2 - 40 + Math.sin(angle) * 260;
      ctx.strokeRect(rx - 35, ry - 35, 70, 70);
    }
    ctx.restore();

    // Scene Specific Visual Rendering
    if (currentChapter === 0) {
      drawSceneQuran(w, h, animTick);
    } else if (currentChapter === 1) {
      drawSceneTasbeeh(w, h, animTick);
    } else if (currentChapter === 2) {
      drawSceneAzkar(w, h, animTick);
    } else if (currentChapter === 3) {
      drawSceneHadith(w, h, animTick);
    } else if (currentChapter === 4) {
      drawScenePrayer(w, h, animTick);
    } else if (currentChapter === 5) {
      drawSceneOutro(w, h, animTick);
    }

    // Dynamic Subtitle Narration Bar across all scenes
    drawSubtitleBar(w, h, ch.headline);

    if (isPlaying) {
      animationFrameId = requestAnimationFrame(drawFrame);
    }
  }

  // Draw Subtitle Bar directly on canvas (Ultra Sharp 1080p)
  function drawSubtitleBar(w, h, text) {
    const subY = h - 90;
    const cx = w / 2;
    ctx.save();
    ctx.fillStyle = 'rgba(2, 12, 7, 0.94)';
    drawRoundRect(ctx, cx - 750, subY - 45, 1500, 90, 45);
    ctx.fill();
    ctx.strokeStyle = '#D4AF37';
    ctx.lineWidth = 3;
    ctx.stroke();

    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 34px "Amiri", "Kitab", sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(text, cx, subY);
    ctx.restore();
  }

  // Visual Scene 0: The Holy Quran (Full HD 1080p)
  function drawSceneQuran(w, h, t) {
    const cx = w / 2;
    const cy = h / 2 - 40;

    // Glowing Quranic Medallion
    const pulse = Math.sin(t * 2) * 16;
    const medGrad = ctx.createRadialGradient(cx, cy, 50, cx, cy, 380 + pulse);
    medGrad.addColorStop(0, 'rgba(212, 175, 55, 0.55)');
    medGrad.addColorStop(1, 'transparent');
    ctx.fillStyle = medGrad;
    ctx.beginPath();
    ctx.arc(cx, cy, 380 + pulse, 0, Math.PI * 2);
    ctx.fill();

    // Open Book Silhouette (Large & Crisp)
    ctx.fillStyle = '#FFFDF7';
    ctx.strokeStyle = '#C5A059';
    ctx.lineWidth = 6;
    drawRoundRect(ctx, cx - 520, cy - 250, 1040, 500, 32);
    ctx.fill();
    ctx.stroke();

    // Book Center Spine & Page Divider
    ctx.strokeStyle = '#947030';
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.moveTo(cx, cy - 250);
    ctx.lineTo(cx, cy + 250);
    ctx.stroke();

    // Calligraphy Verse
    ctx.fillStyle = '#064E3B';
    ctx.font = 'bold 58px "Amiri", "Kitab", serif';
    ctx.textAlign = 'center';
    ctx.fillText('﴿ إِنَّا نَحْنُ نَزَّلْنَا الذِّكْرَ وَإِنَّا لَهُ لَحَافِظُونَ ﴾', cx, cy - 45);

    ctx.fillStyle = '#475569';
    ctx.font = 'bold 34px sans-serif';
    ctx.fillText('سورة الحجر • الآية ٩', cx, cy + 40);

    // Audio Equalizer Visualizer Waves
    const barCount = 28;
    const barWidth = 14;
    const barGap = 26;
    const startX = cx - (barCount * barGap) / 2;
    ctx.fillStyle = '#D97706';
    for (let i = 0; i < barCount; i++) {
      const bh = Math.abs(Math.sin(t * 3 + i * 0.4)) * 90 + 20;
      ctx.fillRect(startX + i * barGap, cy + 140 - bh / 2, barWidth, bh);
    }

    // Floating Feature Badges
    drawAdBadge(cx - 680, cy - 230 + Math.sin(t) * 8, '📖 مصحف المدينة ٦٠٤ صفحة بالرسم العثماني');
    drawAdBadge(cx + 200, cy - 230 + Math.cos(t) * 8, '🎙️ تلاوات خاشعة متصلة لأشهر القراء');
    drawAdBadge(cx - 520, cy + 270 + Math.sin(t * 1.5) * 6, '🔁 المحفظ الآلي لتكرار وتثبيت الآيات');
    drawAdBadge(cx + 40, cy + 270 + Math.cos(t * 1.5) * 6, '🤖 مساعد التدبر بالذكاء الاصطناعي');
  }

  // Visual Scene 1: Smart Tasbeeh (Full HD 1080p)
  function drawSceneTasbeeh(w, h, t) {
    const cx = w / 2;
    const cy = h / 2 - 40;

    // 3D Circular Beaded Ring
    const beadRadius = 280;
    const totalBeads = 33;
    for (let i = 0; i < totalBeads; i++) {
      const angle = (i * 2 * Math.PI / totalBeads) + t * 0.4;
      const bx = cx + Math.cos(angle) * beadRadius;
      const by = cy + Math.sin(angle) * (beadRadius * 0.65);

      const bGrad = ctx.createRadialGradient(bx - 8, by - 8, 4, bx, by, 26);
      bGrad.addColorStop(0, '#FFFBEB');
      bGrad.addColorStop(0.3, '#FDE68A');
      bGrad.addColorStop(0.7, '#D97706');
      bGrad.addColorStop(1, '#78350F');

      ctx.fillStyle = bGrad;
      ctx.beginPath();
      ctx.arc(bx, by, 24, 0, Math.PI * 2);
      ctx.fill();
    }

    // Center Counter Dial
    ctx.fillStyle = 'rgba(9, 28, 20, 0.95)';
    ctx.strokeStyle = '#F59E0B';
    ctx.lineWidth = 8;
    ctx.beginPath();
    ctx.arc(cx, cy, 180, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    // Simulated Counting Number
    const countVal = Math.floor((t * 22) % 100);
    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 96px monospace';
    ctx.textAlign = 'center';
    ctx.fillText(String(countVal).padStart(2, '0'), cx, cy + 22);

    ctx.fillStyle = '#FDE68A';
    ctx.font = 'bold 30px sans-serif';
    ctx.fillText('سبحة الأذكار الذكية', cx, cy + 80);

    // Dhikr Banner
    ctx.fillStyle = '#F8FAFC';
    ctx.font = 'bold 48px "Amiri", "Kitab", serif';
    ctx.fillText('« سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ »', cx, cy + 300);

    drawAdBadge(cx - 680, cy - 140, '⚡ محاكاة فيزيائية واقعية للخرز الطبيعي');
    drawAdBadge(cx + 200, cy - 140, '🎯 أنماط الـ ٣٣ والمائة والتسبيح المخصص');
  }

  // Visual Scene 2: Azkar & Adhkar (Full HD 1080p)
  function drawSceneAzkar(w, h, t) {
    const cx = w / 2;
    const cy = h / 2 - 40;

    // Sun & Moon Visuals
    ctx.save();
    const sunGrad = ctx.createRadialGradient(cx - 460, cy - 100, 15, cx - 460, cy - 100, 130);
    sunGrad.addColorStop(0, '#FBBF24');
    sunGrad.addColorStop(1, 'transparent');
    ctx.fillStyle = sunGrad;
    ctx.beginPath();
    ctx.arc(cx - 460, cy - 100, 130, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();

    // Azkar Card Display
    ctx.fillStyle = '#FFFDF7';
    ctx.strokeStyle = '#10B981';
    ctx.lineWidth = 6;
    drawRoundRect(ctx, cx - 520, cy - 230, 1040, 460, 32);
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = '#065F46';
    ctx.font = 'bold 44px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('🌅 أذكار الصباح والمساء التلقائية', cx, cy - 130);

    ctx.fillStyle = '#1E293B';
    ctx.font = 'bold 46px "Amiri", "Kitab", serif';
    ctx.fillText('« أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ ، وَالْحَمْدُ لِلَّهِ »', cx, cy - 10);

    // Category Grid Pills
    const cats = ['أذكار الصباح', 'أذكار المساء', 'أذكار النوم', 'أذكار الصلاة', 'الرقية الشرعية'];
    cats.forEach((c, idx) => {
      const px = cx - 380 + idx * 190;
      drawMiniPill(px, cy + 110, c);
    });

    drawAdBadge(cx - 620, cy + 280, '📚 ١٤٩ باباً مصنفاً من صحيح السنة النبوية');
    drawAdBadge(cx + 140, cy + 280, '📶 تعمل بالكامل محلياً بدون إنترنت');
  }

  // Visual Scene 3: Hadith Encyclopedia (Full HD 1080p)
  function drawSceneHadith(w, h, t) {
    const cx = w / 2;
    const cy = h / 2 - 40;

    // Manuscript Container
    ctx.fillStyle = '#F8FAFC';
    ctx.strokeStyle = '#3B82F6';
    ctx.lineWidth = 6;
    drawRoundRect(ctx, cx - 550, cy - 230, 1100, 460, 32);
    ctx.fill();
    ctx.stroke();

    // Authentic Hadith Seal Badge
    ctx.fillStyle = '#1E40AF';
    ctx.font = 'bold 42px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('📜 موسوعة الأحاديث النبوية المعتمدة', cx, cy - 135);

    ctx.fillStyle = '#0F172A';
    ctx.font = 'bold 48px "Amiri", "Kitab", serif';
    ctx.fillText('« إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى »', cx, cy - 25);

    ctx.fillStyle = '#475569';
    ctx.font = 'bold 34px sans-serif';
    ctx.fillText('صحيح البخاري [رقم ١] • متفق عليه', cx, cy + 60);

    // Book Badges
    drawMiniPill(cx - 390, cy + 130, 'صحيح البخاري');
    drawMiniPill(cx - 130, cy + 130, 'صحيح مسلم');
    drawMiniPill(cx + 130, cy + 130, 'الأربعين النووية');
    drawMiniPill(cx + 390, cy + 130, 'رياض الصالحين');

    drawAdBadge(cx - 650, cy + 280, '🔍 بحث فوري في أكثر من ٥٠,٠٠٠ حديث محقق');
    drawAdBadge(cx + 170, cy + 280, '🎙️ استماع صوتي وشروح موثوقة لأهل السنة');
  }

  // Visual Scene 4: Prayer Times & Qibla Compass (Full HD 1080p)
  function drawScenePrayer(w, h, t) {
    const cx = w / 2;
    const cy = h / 2 - 40;

    // Rotating Qibla Compass Dial
    const cr = 200;
    ctx.fillStyle = '#081C14';
    ctx.strokeStyle = '#FBBF24';
    ctx.lineWidth = 6;
    ctx.beginPath();
    ctx.arc(cx - 280, cy, cr, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    // Compass Needle pointing towards Mecca
    const needleAngle = Math.sin(t * 1.5) * 0.15 - Math.PI / 2;
    ctx.save();
    ctx.translate(cx - 280, cy);
    ctx.rotate(needleAngle);
    // Green Tip
    ctx.fillStyle = '#10B981';
    ctx.beginPath();
    ctx.moveTo(0, -160);
    ctx.lineTo(20, 0);
    ctx.lineTo(-20, 0);
    ctx.closePath();
    ctx.fill();
    // Red Tip
    ctx.fillStyle = '#EF4444';
    ctx.beginPath();
    ctx.moveTo(0, 160);
    ctx.lineTo(20, 0);
    ctx.lineTo(-20, 0);
    ctx.closePath();
    ctx.fill();
    ctx.restore();

    // Kaaba Icon in center
    ctx.fillStyle = '#FDE68A';
    ctx.font = 'bold 30px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('القبلة المشرفة', cx - 280, cy + 120);

    // Prayer Schedule Card
    ctx.fillStyle = '#FFFDF7';
    ctx.strokeStyle = '#C5A059';
    ctx.lineWidth = 5;
    drawRoundRect(ctx, cx + 50, cy - 220, 520, 440, 30);
    ctx.fill();
    ctx.stroke();

    const prayers = [
      { n: 'الفجر', tm: '04:45 ص' },
      { n: 'الظهر', tm: '12:15 م' },
      { n: 'العصر', tm: '03:40 م' },
      { n: 'المغرب', tm: '06:10 م' },
      { n: 'العشاء', tm: '07:40 م' }
    ];

    ctx.font = 'bold 32px sans-serif';
    prayers.forEach((p, i) => {
      const py = cy - 145 + i * 72;
      ctx.fillStyle = i === 1 ? '#059669' : '#334155';
      ctx.textAlign = 'right';
      ctx.fillText(p.n, cx + 220, py);
      ctx.textAlign = 'left';
      ctx.fillText(p.tm, cx + 330, py);
    });

    drawAdBadge(cx - 650, cy + 280, '⏰ منبه الأذان الفلكي الدقيق لجميع مدن العالم');
    drawAdBadge(cx + 170, cy + 280, '🧭 بوصلة القبلة المشرفة أينما كنت');
  }

  // Visual Scene 5: Outro & Commercial Google Play Call to Action (Full HD 1080p)
  function drawSceneOutro(w, h, t) {
    const cx = w / 2;
    const cy = h / 2 - 50;

    // Radiant Gold Rays Burst
    ctx.save();
    for (let i = 0; i < 16; i++) {
      const ang = (i * 2 * Math.PI / 16) + t * 0.15;
      ctx.fillStyle = 'rgba(212, 175, 55, 0.12)';
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.arc(cx, cy, 650, ang, ang + 0.2);
      ctx.closePath();
      ctx.fill();
    }
    ctx.restore();

    // App Logo Image or Styled Medallion
    const logoR = 110;
    ctx.save();
    ctx.beginPath();
    ctx.arc(cx, cy - 40, logoR, 0, Math.PI * 2);
    ctx.closePath();
    ctx.clip();
    if (appLogoImg.complete && appLogoImg.naturalWidth > 0) {
      ctx.drawImage(appLogoImg, cx - logoR, cy - 40 - logoR, logoR * 2, logoR * 2);
    } else {
      const grad = ctx.createLinearGradient(cx - logoR, cy - 40 - logoR, cx + logoR, cy - 40 + logoR);
      grad.addColorStop(0, '#1B4D3E');
      grad.addColorStop(1, '#064E3B');
      ctx.fillStyle = grad;
      ctx.fill();
    }
    ctx.restore();

    // Gold Ring around logo
    ctx.strokeStyle = '#F59E0B';
    ctx.lineWidth = 8;
    ctx.beginPath();
    ctx.arc(cx, cy - 40, logoR, 0, Math.PI * 2);
    ctx.stroke();

    // Grand Ad Headline
    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 64px "Amiri", "Kitab", serif';
    ctx.textAlign = 'center';
    ctx.fillText('تطبيق سبّح بخشوع', cx, cy + 140);

    // Stars & Rating
    ctx.fillStyle = '#FBBF24';
    ctx.font = 'bold 34px sans-serif';
    ctx.fillText('★★★★★ 4.9 • رفيقك اليومي إلى الجنة • مجاني وبدون إعلانات', cx, cy + 205);

    // Google Play Button Graphic on Canvas
    const btnW = 700;
    const btnH = 88;
    const btnGrad = ctx.createLinearGradient(cx - btnW / 2, cy + 250, cx + btnW / 2, cy + 250 + btnH);
    btnGrad.addColorStop(0, '#00C1A4');
    btnGrad.addColorStop(1, '#059669');
    ctx.fillStyle = btnGrad;
    drawRoundRect(ctx, cx - btnW / 2, cy + 250, btnW, btnH, 44);
    ctx.fill();

    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 36px sans-serif';
    ctx.textBaseline = 'middle';
    ctx.fillText('حمّل الآن من Google Play مجاناً ↗', cx, cy + 294);
  }

  // Draw floating glowing badge on canvas (Full HD 1080p)
  function drawAdBadge(x, y, text) {
    ctx.save();
    ctx.fillStyle = 'rgba(10, 26, 18, 0.94)';
    ctx.strokeStyle = 'rgba(212, 175, 55, 0.75)';
    ctx.lineWidth = 3;
    drawRoundRect(ctx, x, y, 480, 60, 30);
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = '#FDE68A';
    ctx.font = 'bold 24px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(text, x + 240, y + 30);
    ctx.restore();
  }

  function drawMiniPill(x, y, text) {
    ctx.save();
    ctx.fillStyle = '#E2E8F0';
    drawRoundRect(ctx, x - 85, y - 24, 170, 48, 24);
    ctx.fill();

    ctx.fillStyle = '#1E293B';
    ctx.font = 'bold 22px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(text, x, y);
    ctx.restore();
  }

  // 2. Play / Pause & Navigation
  function play() {
    isPlaying = true;
    if (playOverlay) playOverlay.style.display = 'none';
    if (iconPlay) iconPlay.style.display = 'none';
    if (iconPause) iconPause.style.display = 'block';

    if (!isMuted) {
      speakChapter(chapters[currentChapter].speechText);
    }
    startTimer();
    drawFrame();
  }

  function pause() {
    isPlaying = false;
    if (playOverlay) playOverlay.style.display = 'flex';
    if (iconPlay) iconPlay.style.display = 'block';
    if (iconPause) iconPause.style.display = 'none';

    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    clearInterval(timerInterval);
    if (animationFrameId) cancelAnimationFrame(animationFrameId);
    drawFrame();
  }

  function togglePlay() {
    if (isPlaying) pause();
    else play();
  }

  function speakChapter(text) {
    if (!('speechSynthesis' in window) || isMuted) return;
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'ar-SA';
    utterance.rate = 1.0;
    const voices = window.speechSynthesis.getVoices();
    const arVoice = voices.find(v => v.lang.startsWith('ar') || v.lang.includes('Arabic'));
    if (arVoice) utterance.voice = arVoice;
    window.speechSynthesis.speak(utterance);
  }

  function startTimer() {
    clearInterval(timerInterval);
    timerInterval = setInterval(() => {
      chapterTimerSeconds++;
      const overallSeconds = (currentChapter * CHAPTER_DURATION) + chapterTimerSeconds;
      const totalSeconds = chapters.length * CHAPTER_DURATION;
      const pct = (overallSeconds / totalSeconds) * 100;
      if (timelineFill) timelineFill.style.width = `${pct}%`;

      const m = Math.floor(overallSeconds / 60);
      const sec = overallSeconds % 60;
      if (timeDisplay) {
        timeDisplay.textContent = `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')} / 01:30`;
      }

      if (chapterTimerSeconds >= CHAPTER_DURATION) {
        chapterTimerSeconds = 0;
        if (currentChapter < chapters.length - 1) {
          goToChapter(currentChapter + 1);
        } else {
          // Finished entire video ad
          pause();
          chapterTimerSeconds = 0;
          if (timelineFill) timelineFill.style.width = '100%';
        }
      }
    }, 1000);
  }

  function goToChapter(index) {
    currentChapter = Math.max(0, Math.min(chapters.length - 1, index));
    chapterTimerSeconds = 0;

    const ch = chapters[currentChapter];
    if (sceneTagEl) sceneTagEl.textContent = ch.tag;
    if (sceneHeadlineEl) sceneHeadlineEl.textContent = ch.headline;

    chapterButtons.forEach((btn, i) => {
      btn.classList.toggle('active', i === currentChapter);
    });

    const overallSeconds = currentChapter * CHAPTER_DURATION;
    const totalSeconds = chapters.length * CHAPTER_DURATION;
    if (timelineFill) timelineFill.style.width = `${(overallSeconds / totalSeconds) * 100}%`;
    const m = Math.floor(overallSeconds / 60);
    const sec = overallSeconds % 60;
    if (timeDisplay) {
      timeDisplay.textContent = `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')} / 01:30`;
    }

    if (isPlaying && !isMuted) {
      speakChapter(ch.speechText);
    }

    drawFrame();
  }

  // 3. Event Listeners
  if (playOverlay) playOverlay.addEventListener('click', play);
  if (playPauseBtn) playPauseBtn.addEventListener('click', togglePlay);
  if (canvas) canvas.addEventListener('click', togglePlay);

  if (muteBtn) {
    muteBtn.addEventListener('click', () => {
      isMuted = !isMuted;
      if (iconSoundOn) iconSoundOn.style.display = isMuted ? 'none' : 'block';
      if (iconSoundOff) iconSoundOff.style.display = isMuted ? 'block' : 'none';
      if (isMuted && 'speechSynthesis' in window) {
        window.speechSynthesis.cancel();
      } else if (!isMuted && isPlaying) {
        speakChapter(chapters[currentChapter].speechText);
      }
      showAppToast(isMuted ? 'تم كتم الصوت الإعلاني' : 'تم تفعيل الصوت والتعليق الإعلاني', 'info');
    });
  }

  if (fullscreenBtn && stageContainer) {
    fullscreenBtn.addEventListener('click', () => {
      if (!document.fullscreenElement) {
        stageContainer.requestFullscreen().catch(() => {});
      } else {
        document.exitFullscreen().catch(() => {});
      }
    });
  }

  if (timelineWrap) {
    timelineWrap.addEventListener('click', (e) => {
      const rect = timelineWrap.getBoundingClientRect();
      const clickX = e.clientX - rect.left;
      const ratio = Math.max(0, Math.min(1, clickX / rect.width));
      const targetSec = ratio * (chapters.length * CHAPTER_DURATION);
      const targetChap = Math.floor(targetSec / CHAPTER_DURATION);
      chapterTimerSeconds = Math.floor(targetSec % CHAPTER_DURATION);
      goToChapter(targetChap);
    });
  }

  chapterButtons.forEach((btn, idx) => {
    btn.addEventListener('click', () => {
      goToChapter(idx);
    });
  });

  if (shareBtn) {
    shareBtn.addEventListener('click', () => {
      const promoText = `تطبيق "سبّح بخشوع" 🌿\nرفيقك اليومي للقرآن الكريم، السبحة الذكية، موسوعة الأذكار والأحاديث، ومواقيت الصلاة.\nمجاني تماماً وبدون إعلانات على Google Play:\nhttps://play.google.com/store/apps/details?id=com.sabah.bikhushue`;
      if (navigator.clipboard) {
        navigator.clipboard.writeText(promoText).then(() => {
          showAppToast('✅ تم نسخ رابط الإعلان والتطبيق للمشاركة!', 'success');
        });
      }
    });
  }

  // Draw initial scene 0 preview immediately so user sees graphics on home screen
  goToChapter(0);

  // 4. Modal Opening & Closing Control
  const tutorialModal = document.getElementById('modalTutorialVideo');
  const btnCloseTut = document.getElementById('btnCloseTutorialModal');
  const cardTutorial = document.getElementById('cardTutorialVideo');

  function openTutorialModal() {
    if (tutorialModal) {
      tutorialModal.classList.add('open');
      // Ensure canvas is drawn and start playing
      goToChapter(0);
      play();
    }
  }

  function closeTutorialModal() {
    if (tutorialModal) {
      tutorialModal.classList.remove('open');
      pause();
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
      }
    }
  }

  if (cardTutorial) {
    cardTutorial.addEventListener('click', openTutorialModal);
  }
  if (btnCloseTut) {
    btnCloseTut.addEventListener('click', closeTutorialModal);
  }
  if (tutorialModal) {
    tutorialModal.addEventListener('click', (e) => {
      if (e.target === tutorialModal) {
        closeTutorialModal();
      }
    });
  }

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && tutorialModal && tutorialModal.classList.contains('open')) {
      closeTutorialModal();
    }
  });

  window.openTutorialVideoModal = openTutorialModal;
  window.closeTutorialVideoModal = closeTutorialModal;
}


// =========================================================================
// Universal 8-Theme Visual System (طِبق الأصل 100% من أندرويد ThemeHelper.kt)
// =========================================================================
function getThemeBeadColors() {
  const t = localStorage.getItem('sb_app_theme') || 'creamy';
  if (t === 'lunar') {
    return {
      counted: { c0: '#FFFFFF', c1: '#E0F2F1', c2: '#80CBC4', shadow: 'rgba(128, 203, 196, 0.85)', stroke: '#80CBC4' },
      active: { c0: '#E0F7FA', c1: '#80DEEA', c2: '#26A69A', c3: '#004D40', shadow: 'rgba(38, 166, 154, 0.95)', stroke: '#80CBC4' },
      idle: { c0: '#90A4AE', c1: '#607D8B', c2: '#37474F', stroke: 'rgba(55, 71, 79, 0.8)' },
      string: '#546E7A',
      dangler: { c0: '#E0F7FA', c1: '#80CBC4', c2: '#004D40' },
      mode100Counted: '#80CBC4',
      mode100Active: '#E0F7FA',
      mode100Idle: '#607D8B'
    };
  } else if (t === 'burgundy') {
    return {
      counted: { c0: '#FFFDF5', c1: '#FDE68A', c2: '#D4AF37', shadow: 'rgba(212, 175, 55, 0.9)', stroke: '#D4AF37' },
      active: { c0: '#FFFBEB', c1: '#F59E0B', c2: '#B45309', c3: '#78350F', shadow: 'rgba(245, 158, 11, 0.95)', stroke: '#FDE68A' },
      idle: { c0: '#9F1239', c1: '#881337', c2: '#4C0519', stroke: 'rgba(76, 5, 25, 0.8)' },
      string: '#D4AF37',
      dangler: { c0: '#FFFBEB', c1: '#D4AF37', c2: '#78350F' },
      mode100Counted: '#D4AF37',
      mode100Active: '#FFFBEB',
      mode100Idle: '#881337'
    };
  } else if (t === 'night') {
    return {
      counted: { c0: '#F0FDF4', c1: '#86EFAC', c2: '#22C55E', shadow: 'rgba(34, 197, 94, 0.8)', stroke: '#4ADE80' },
      active: { c0: '#DCFCE7', c1: '#4ADE80', c2: '#16A34A', c3: '#14532D', shadow: 'rgba(74, 222, 128, 0.9)', stroke: '#86EFAC' },
      idle: { c0: '#64748B', c1: '#334155', c2: '#1E293B', stroke: 'rgba(30, 41, 59, 0.8)' },
      string: '#475569',
      dangler: { c0: '#DCFCE7', c1: '#22C55E', c2: '#14532D' },
      mode100Counted: '#4ADE80',
      mode100Active: '#FFFFFF',
      mode100Idle: '#475569'
    };
  } else if (t === 'sky') {
    return {
      counted: { c0: '#FFFFFF', c1: '#BAE6FD', c2: '#0284C7', shadow: 'rgba(2, 132, 199, 0.75)', stroke: '#38BDF8' },
      active: { c0: '#E0F2FE', c1: '#38BDF8', c2: '#0369A1', c3: '#0C4A6E', shadow: 'rgba(56, 189, 248, 0.9)', stroke: '#BAE6FD' },
      idle: { c0: '#7DD3FC', c1: '#38BDF8', c2: '#0284C7', stroke: 'rgba(3, 105, 161, 0.6)' },
      string: '#0284C7',
      dangler: { c0: '#E0F2FE', c1: '#38BDF8', c2: '#0C4A6E' },
      mode100Counted: '#38BDF8',
      mode100Active: '#FFFFFF',
      mode100Idle: '#0284C7'
    };
  } else if (t === 'pink') {
    return {
      counted: { c0: '#FFFFFF', c1: '#FCE7F3', c2: '#DB2777', shadow: 'rgba(219, 39, 119, 0.75)', stroke: '#F472B6' },
      active: { c0: '#FDF2F8', c1: '#F472B6', c2: '#BE185D', c3: '#831843', shadow: 'rgba(244, 114, 182, 0.9)', stroke: '#FCE7F3' },
      idle: { c0: '#F9A8D4', c1: '#F472B6', c2: '#DB2777', stroke: 'rgba(190, 24, 93, 0.6)' },
      string: '#DB2777',
      dangler: { c0: '#FDF2F8', c1: '#F472B6', c2: '#831843' },
      mode100Counted: '#F472B6',
      mode100Active: '#FFFFFF',
      mode100Idle: '#DB2777'
    };
  } else if (t === 'crimson') {
    return {
      counted: { c0: '#FFFFFF', c1: '#FFE4E6', c2: '#E11D48', shadow: 'rgba(225, 29, 72, 0.75)', stroke: '#FB7185' },
      active: { c0: '#FFF1F2', c1: '#FB7185', c2: '#BE123C', c3: '#881337', shadow: 'rgba(251, 113, 133, 0.9)', stroke: '#FFE4E6' },
      idle: { c0: '#FDA4AF', c1: '#FB7185', c2: '#E11D48', stroke: 'rgba(190, 18, 60, 0.6)' },
      string: '#E11D48',
      dangler: { c0: '#FFF1F2', c1: '#FB7185', c2: '#881337' },
      mode100Counted: '#FB7185',
      mode100Active: '#FFFFFF',
      mode100Idle: '#E11D48'
    };
  }
  // Default creamy / emerald
  return {
    counted: { c0: '#FFFFFF', c1: '#FDF2E9', c2: '#D4AF37', shadow: 'rgba(212, 175, 55, 0.7)', stroke: '#D4AF37' },
    active: { c0: '#FFF275', c1: '#FFD700', c2: '#10B981', c3: '#064E3B', shadow: 'rgba(16, 185, 129, 0.9)', stroke: '#FFD700' },
    idle: { c0: '#A7F3D0', c1: '#34D399', c2: '#065F46', stroke: 'rgba(6, 95, 70, 0.6)' },
    string: '#8C5C38',
    dangler: { c0: '#FFF275', c1: '#D4AF37', c2: '#8B6508' },
    mode100Counted: '#FFD700',
    mode100Active: '#FFFFFF',
    mode100Idle: '#34D399'
  };
}

function applyTheme(themeKey) {
  const validThemes = ['creamy', 'night', 'lunar', 'burgundy', 'emerald', 'sky', 'pink', 'crimson'];
  if (!validThemes.includes(themeKey)) themeKey = 'creamy';

  // 1. Remove all old theme classes
  validThemes.forEach(t => document.body.classList.remove(`theme-${t}`));
  document.body.classList.remove('theme-dark', 'theme-white');

  // 2. Add current theme class
  document.body.classList.add(`theme-${themeKey}`);
  if (['night', 'lunar', 'burgundy'].includes(themeKey)) {
    document.body.classList.add('theme-dark');
  }

  // 3. Save to storage
  try {
    localStorage.setItem('sb_app_theme', themeKey);
  } catch (_) {}
  if (typeof state !== 'undefined' && state.quran) {
    state.quran.theme = themeKey;
  }

  // 4. Update active button indicators
  document.querySelectorAll('.theme-choice-btn').forEach(b => {
    b.classList.toggle('active', b.getAttribute('data-theme') === themeKey);
  });

  // 5. Re-render beads if tasbeeh is available
  if (window.renderTasbeehEngine) {
    try { window.renderTasbeehEngine(); } catch (_) {}
  }
}
window.applyTheme = applyTheme;

function initThemeSystem() {
  const savedTheme = localStorage.getItem('sb_app_theme') || 'creamy';
  applyTheme(savedTheme);

  // Wire up theme choice buttons in themesGridPicker
  const picker = document.getElementById('themesGridPicker');
  if (picker) {
    picker.querySelectorAll('.theme-choice-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const theme = btn.getAttribute('data-theme');
        if (theme) applyTheme(theme);
      });
    });
  }
}
window.initThemeSystem = initThemeSystem;

// --- 7. Comprehensive Tasbeeh Engine (السبحة الذكية المتطورة مع التوثيق الكامل) ---
// مطابقة 100% لتطبيق أندرويد TasbeehActivity و SpiralTasbeehActivity مع بطاقتي 33 و 100 وكروت الأذكار
function setupTasbeehEngine() {
  const canvas = document.getElementById('beadsCanvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');

  // Elements
  const cardMode33 = document.getElementById('cardMode33');
  const cardMode100 = document.getElementById('cardMode100');
  const indicatorMode33 = document.getElementById('indicatorMode33');
  const indicatorMode100 = document.getElementById('indicatorMode100');
  const goalPills = document.querySelectorAll('.goal-pill');

  const activePhraseEl = document.getElementById('tasbeehActivePhrase');
  const activeVirtueEl = document.getElementById('tasbeehActiveVirtue');
  const activeProofTextEl = document.getElementById('tasbeehActiveProofText');
  const btnPrevZikr = document.getElementById('btnPrevZikr');
  const btnNextZikr = document.getElementById('btnNextZikr');

  // Center Misbaha Display & Hover Tooltip Elements
  const tasbeehInnerPhraseWrap = document.getElementById('tasbeehInnerPhraseWrap');
  const tasbeehInnerPhrase = document.getElementById('tasbeehInnerPhrase');
  const tasbeehHoverTooltip = document.getElementById('tasbeehHoverTooltip');
  const tooltipZikrTitle = document.getElementById('tooltipZikrTitle');
  const tooltipZikrVirtue = document.getElementById('tooltipZikrVirtue');
  const tooltipZikrProof = document.getElementById('tooltipZikrProof');

  const beadsCanvasWrapper = document.getElementById('tasbeehCanvasWrapper');
  const beadsCenterCount = document.getElementById('beadsCenterCount');
  const beadsCenterTarget = document.getElementById('beadsCenterTarget');
  const beadsHundredsBadge = document.getElementById('beadsHundredsBadge');

  const btnTapTasbeeh = document.getElementById('btnTapTasbeeh');
  const btnResetTasbeeh = document.getElementById('btnResetTasbeeh');
  const btnToggleTasbeehSound = document.getElementById('btnToggleTasbeehSound');
  const tasbeehSoundIcon = document.getElementById('tasbeehSoundIcon');
  const tasbeehSoundLabel = document.getElementById('tasbeehSoundLabel');
  const btnOpenAddZikrModal = document.getElementById('btnOpenAddZikrModal');

  // Symmetrical Sanctuary Columns (Right & Left)
  const sideRightContainer = document.getElementById('tasbeehSideRight');
  const sideLeftContainer = document.getElementById('tasbeehSideLeft');
  const cardsContainer = document.getElementById('tasbeehCardsContainer');
  const zikrListCountBadge = document.getElementById('zikrListCountBadge');

  // Modal elements
  const modalAddCustomZikr = document.getElementById('modalAddCustomZikr');
  const btnCloseAddZikrModal = document.getElementById('btnCloseAddZikrModal');
  const btnCancelAddZikr = document.getElementById('btnCancelAddZikr');
  const btnConfirmAddZikr = document.getElementById('btnConfirmAddZikr');
  const inputCustomZikrText = document.getElementById('inputCustomZikrText');
  const inputCustomZikrVirtue = document.getElementById('inputCustomZikrVirtue');

  const toArDigits = (num) => String(num).replace(/[0-9]/g, d => ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'][parseInt(d, 10)]);

  // Default Azkar with Authentic Hadith / Quran Proofs
  const defaultZikrs = [
    {
      id: 'subhanallah',
      phrase: 'سُبْحَانَ اللَّهِ',
      virtue: 'أحب الكلام إلى الله تعالى، وغراس الجنة، وعشرون حسنة وحط عشرين سيئة.',
      proof: 'قال رسول الله ﷺ: «أَيَعْجِزُ أَحَدُكُمْ أَنْ يَكْسِبَ كُلَّ يَوْمٍ أَلْفَ حَسَنَةٍ؟ يُسَبِّحُ مِائَةَ تَسْبِيحَةٍ فَيُكْتَبُ لَهُ أَلْفُ حَسَنَةٍ، أَوْ يُحَطُّ عَنْهُ أَلْفُ خَطِيئَةٍ» [رواه مسلم (2698)]، وقال ﷺ في تسبيح دبر الصلوات ثلاثاً وثلاثين: «غُفِرَتْ خَطَايَاهُ وَإِنْ كَانَتْ مِثْلَ زَبَدِ الْبَحْرِ» [صحيح مسلم (597)].',
      goal: 33,
      mode: '33'
    },
    {
      id: 'alhamdulillah',
      phrase: 'الْحَمْدُ لِلَّهِ',
      virtue: 'تملأ الميزان بالخيرات، وأفضل الدعاء، وثناء عظيم على نعم الله عز وجل.',
      proof: 'قال رسول الله ﷺ: «وَالْحَمْدُ لِلَّهِ تَمْلأُ الْمِيزَانَ، وَسُبْحَانَ اللَّهِ وَالْحَمْدُ لِلَّهِ تَمْلآنِ -أَوْ تَمْلأُ- مَا بَيْنَ السَّمَاوَاتِ وَالأَرْضِ» [رواه مسلم (223)]، وقال ﷺ: «أَفْضَلُ الذِّكْرِ لَا إِلَهَ إِلَّا اللَّهُ، وَأَفْضَلُ الدُّعَاءِ الْحَمْدُ لِلَّهِ» [رواه الترمذي وحسنه].',
      goal: 33,
      mode: '33'
    },
    {
      id: 'allahuakbar',
      phrase: 'اللَّهُ أَكْبَرُ',
      virtue: 'كبرياء الله وعظمته، وتملأ ما بين السماء والأرض، ودبر كل صلاة مكتوبة وعند النوم.',
      proof: 'عن النبي ﷺ في التسبيح والتحميد والتكبير دبر الصلوات المكتوبة: «تُسَبِّحُونَ وَتَحْمَدُونَ وَتُكَبِّرُونَ خَلْفَ كُلِّ صَلَاةٍ ثَلَاثًا وَثَلَاثِينَ» [متفق عليه: البخاري ومسلم]، وعند النوم ٣٤ تكبيرة [متفق عليه].',
      goal: 33,
      mode: '33'
    },
    {
      id: 'istighfar',
      phrase: 'أَسْتَغْفِرُ اللَّهَ',
      virtue: 'مفتاح الرزق وممحاة الذنوب، ومفرّج الكروب، وسنة سيد المرسلين ﷺ.',
      proof: 'قال الله تعالى: ﴿فَقُلْتُ اسْتَغْفِرُوا رَبَّكُمْ إِنَّهُ كَانَ غَفَّارًا ۝ يُرْسِلِ السَّمَاءَ عَلَيْكُمْ مِدْرَارًا ۝ وَيُمْدِدْكُمْ بِأَمْوَالٍ وَبَنِينَ﴾ [نوح: 10-12]، وقال النبي ﷺ: «وَاللَّهِ إِنِّي لأَسْتَغْفِرُ اللَّهَ فِي الْيَوْمِ مِائَةَ مَرَّةٍ» [رواه مسلم (2702)].',
      goal: 100,
      mode: '100'
    },
    {
      id: 'subhanallah_wabihamdihi',
      phrase: 'سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ',
      virtue: 'كلمتان خفيفتان على اللسان، ثقيلتان في الميزان، حبيبتان إلى الرحمن.',
      proof: 'قال رسول الله ﷺ: «كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي الْمِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَنِ: سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ» [متفق عليه: البخاري (6682) ومسلم (2694)]، وقال ﷺ: «مَنْ قَالَ: سُبْحَانَ اللَّهِ وَبِحَمْدِهِ فِي يَوْمٍ مِائَةَ مَرَّةٍ حُطَّتْ خَطَايَاهُ وَإِنْ كَانَتْ مِثْلَ زَبَدِ الْبَحْرِ» [متفق عليه].',
      goal: 100,
      mode: '100'
    },
    {
      id: 'la_ilaha_illallah_wahdahu',
      phrase: 'لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ',
      virtue: 'تعدل عتق عشر رقاب، وتكتب مئة حسنة، وتمحى مئة سيئة، وحرز من الشيطان طوال اليوم.',
      proof: 'قال رسول الله ﷺ: «مَنْ قَالَ: لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، فِي يَوْمٍ مِائَةَ مَرَّةٍ كَانَتْ لَهُ عَدْلَ عَشْرِ رِقَابٍ، وَكُتِبَتْ لَهُ مِائَةُ حَسَنَةٍ، وَمُحِيَتْ عَنْهُ مِائَةُ سَيِّئَةٍ، وَكَانَتْ لَهُ حِرْزًا مِنَ الشَّيْطَانِ يَوْمَهُ ذَلِكَ حَتَّى يُمْسِيَ» [متفق عليه: البخاري (3293) ومسلم (2691)].',
      goal: 100,
      mode: '100'
    },
    {
      id: 'salawat',
      phrase: 'اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ',
      virtue: 'من صلى عليه صلاة صلى الله عليه بها عشراً، وكفي همه وغفر ذنبه ونال شفاعته العظمى.',
      proof: 'قال الله تعالى: ﴿إِنَّ اللَّهَ وَمَلَائِكَتَهُ يُصَلُّونَ عَلَى النَّبِيِّ يَا أَيُّهَا الَّذِينَ آمَنُوا صَلُّوا عَلَيْهِ وَسَلِّمُوا تَسْلِيمًا﴾ [الأحزاب: 56]، وقال رسول الله ﷺ: «مَنْ صَلَّى عَلَيَّ صَلَاةً صَلَّى اللَّهُ عَلَيْهِ بِهَا عَشْرًا» [رواه مسلم (408)]، وقال لأبيّ بن كعب: «إِذًا تُكْفَى هَمَّكَ، وَيُغْفَرُ لَكَ ذَنْبُكَ» [الترمذي].',
      goal: 100,
      mode: '100'
    },
    {
      id: 'la_hawla',
      phrase: 'لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ الْعَلِيِّ الْعَظِيمِ',
      virtue: 'كنز عظيم من كنوز الجنة، وباب مبارك لتفريج الكروب والاستعانة بالله تعالى.',
      proof: 'قال رسول الله ﷺ لأبي موسى الأشعري رضي الله عنه: «أَلَا أَدُلُّكَ عَلَى كَلِمَةٍ مِنْ كُنُوزِ الْجَنَّةِ؟ قُلْتُ: بَلَى يَا رَسُولَ اللَّهِ، قَالَ: قُلْ: لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ» [متفق عليه: البخاري (4205) ومسلم (2704)].',
      goal: 100,
      mode: '100'
    },
    {
      id: 'hasbunallah',
      phrase: 'حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ',
      virtue: 'أمان الخائفين وقوة المتوكلين، قالها إبراهيم والنبي صلى الله عليه وسلم في أشد المواقف.',
      proof: 'قال الله تعالى: ﴿الَّذِينَ قَالَ لَهُمُ النَّاسُ إِنَّ النَّاسَ قَدْ جَمَعُوا لَكُمْ فَاخْشَوْهُمْ فَزَادَهُمْ إِيمَانًا وَقَالُوا حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ ۝ فَانْقَلَبُوا بِنِعْمَةٍ مِنَ اللَّهِ وَفَضْلٍ لَمْ يَمْسَسْهُمْ سُوءٌ﴾ [آل عمران: 173-174]، وعن ابن عباس: «قَالَهَا إِبْرَاهِيمُ حِينَ أُلْقِيَ فِي النَّارِ، وَقَالَهَا مُحَمَّدٌ ﷺ حِينَ قَالُوا: إِنَّ النَّاسَ قَدْ جَمَعُوا لَكُمْ» [رواه البخاري (4563)].',
      goal: 100,
      mode: '100'
    },
    {
      id: 'baqiyat_salihat',
      phrase: 'سُبْحَانَ اللَّهِ ، وَالْحَمْدُ لِلَّهِ ، وَلَا إِلَهَ إِلَّا اللَّهُ ، وَاللَّهُ أَكْبَرُ',
      virtue: 'أحب الكلام إلى الله مما طلعت عليه الشمس، وهي الباقيات الصالحات وغراس الجنة.',
      proof: 'قال رسول الله ﷺ: «لَأَنْ أَقُولَ: سُبْحَانَ اللَّهِ، وَالْحَمْدُ لِلَّهِ، وَلَا إِلَهَ إِلَّا اللَّهُ، وَاللَّهُ أَكْبَرُ، أَحَبُّ إِلَيَّ مِمَّا طَلَعَتْ عَلَيْهِ الشَّمْسُ» [رواه مسلم (2695)]، وقال ﷺ: «إِنَّ سُبْحَانَ اللَّهِ، وَالْحَمْدُ لِلَّهِ، وَلَا إِلَهَ إِلَّا اللَّهُ، وَاللَّهُ أَكْبَرُ تَنْفُضُ الْخَطَايَا كَمَا تَنْفُضُ الشَّجَرَةُ وَرَقَهَا» [الترمذي].',
      goal: 33,
      mode: '33'
    }
  ];

  // Load custom zikrs from localStorage
  let customZikrs = [];
  try {
    const savedCustom = localStorage.getItem('sb_custom_zikrs');
    if (savedCustom) customZikrs = JSON.parse(savedCustom);
  } catch (e) {
    customZikrs = [];
  }

  // Combined zikrs
  let allZikrs = [...defaultZikrs, ...customZikrs];

  // Load saved per-zikr counters
  let zikrCounts = {};
  try {
    const savedCounts = localStorage.getItem('sb_zikr_counts');
    if (savedCounts) zikrCounts = JSON.parse(savedCounts);
  } catch (e) {
    zikrCounts = {};
  }

  // State
  let activeIndex = 0;
  const savedActiveIndex = parseInt(localStorage.getItem('sb_active_zikr_index'), 10);
  if (!isNaN(savedActiveIndex) && savedActiveIndex >= 0 && savedActiveIndex < allZikrs.length) {
    activeIndex = savedActiveIndex;
  }

  let currentMode = allZikrs[activeIndex].mode || '33'; // '33' or '100'
  let currentGoal = allZikrs[activeIndex].goal || 33;
  let animOffset = 0;
  let hundredsCount = parseInt(localStorage.getItem('sb_tasbeeh_hundreds') || '0', 10);

  // Hi-DPI Canvas scaling
  const dpr = window.devicePixelRatio || 1;
  canvas.width = 360 * dpr;
  canvas.height = 360 * dpr;
  ctx.scale(dpr, dpr);

  function getCurrentZikr() {
    return allZikrs[activeIndex] || allZikrs[0];
  }

  function getCount(zikrId) {
    return zikrCounts[zikrId] || 0;
  }

  function setCount(zikrId, val) {
    zikrCounts[zikrId] = val;
    try {
      localStorage.setItem('sb_zikr_counts', JSON.stringify(zikrCounts));
    } catch (e) {}
  }

  // --- DRAWING: Mode 33 (Ring of 33 Beads) ---
  function drawBeads33(count) {
    ctx.clearRect(0, 0, 360, 360);
    const cx = 180;
    const cy = 180;
    const radius = 135;
    const total = 33;
    const pal = getThemeBeadColors();

    // 1. Draw connecting strand / wire
    ctx.beginPath();
    ctx.arc(cx, cy, radius, 0, Math.PI * 2);
    ctx.strokeStyle = pal.string;
    ctx.lineWidth = 4;
    ctx.stroke();

    // 2. Draw 33 Beads
    const currentInCycle = currentGoal > 0 ? (count % total) : (count % total);
    for (let i = 0; i < total; i++) {
      const angle = ((i - animOffset) / total) * Math.PI * 2 - Math.PI / 2;
      const bx = cx + Math.cos(angle) * radius;
      const by = cy + Math.sin(angle) * radius;

      const isCounted = i < currentInCycle;
      const isCurrent = i === currentInCycle;

      ctx.save();
      ctx.beginPath();
      const bRadius = isCurrent ? 14 : 11;
      ctx.arc(bx, by, bRadius, 0, Math.PI * 2);

      const grad = ctx.createRadialGradient(bx - 3, by - 3, 2, bx, by, bRadius);
      if (isCounted) {
        grad.addColorStop(0, pal.counted.c0);
        grad.addColorStop(0.5, pal.counted.c1);
        grad.addColorStop(1, pal.counted.c2);
        ctx.fillStyle = grad;
        ctx.shadowColor = pal.counted.shadow;
        ctx.shadowBlur = 9;
        ctx.fill();
        ctx.strokeStyle = pal.counted.stroke;
        ctx.lineWidth = 1.8;
        ctx.stroke();
      } else if (isCurrent) {
        grad.addColorStop(0, pal.active.c0);
        grad.addColorStop(0.4, pal.active.c1);
        grad.addColorStop(0.8, pal.active.c2);
        grad.addColorStop(1, pal.active.c3);
        ctx.fillStyle = grad;
        ctx.shadowColor = pal.active.shadow;
        ctx.shadowBlur = 14;
        ctx.fill();
        ctx.strokeStyle = pal.active.stroke;
        ctx.lineWidth = 2.4;
        ctx.stroke();
      } else {
        grad.addColorStop(0, pal.idle.c0);
        grad.addColorStop(0.5, pal.idle.c1);
        grad.addColorStop(1, pal.idle.c2);
        ctx.fillStyle = grad;
        ctx.fill();
        ctx.strokeStyle = pal.idle.stroke;
        ctx.lineWidth = 1.5;
        ctx.stroke();
      }
      ctx.restore();
    }
  }

  // --- DRAWING: Mode 100 (99 Beads on Rounded Rect + 1 Dangling Bead, matching SpiralBeadsView) ---
  function drawBeads100(count) {
    ctx.clearRect(0, 0, 360, 360);
    const margin = 28;
    const left = margin;
    const top = margin;
    const right = 360 - margin;
    const bottom = 360 - margin;
    const cr = 46; // corner radius
    const pal = getThemeBeadColors();

    // 1. Draw rounded rectangle strand
    ctx.save();
    ctx.beginPath();
    ctx.moveTo(180, top);
    ctx.lineTo(right - cr, top);
    ctx.arcTo(right, top, right, top + cr, cr);
    ctx.lineTo(right, bottom - cr);
    ctx.arcTo(right, bottom, right - cr, bottom, cr);
    ctx.lineTo(left + cr, bottom);
    ctx.arcTo(left, bottom, left, bottom - cr, cr);
    ctx.lineTo(left, top + cr);
    ctx.arcTo(left, top, left + cr, top, cr);
    ctx.closePath();
    ctx.strokeStyle = pal.string;
    ctx.lineWidth = 3.5;
    ctx.stroke();

    // 2. Draw string connecting top-center to the dangling minaret bead
    const danglerX = 180;
    const danglerY = top + 52;
    ctx.beginPath();
    ctx.moveTo(180, top);
    ctx.lineTo(danglerX, danglerY);
    ctx.strokeStyle = pal.string;
    ctx.lineWidth = 3;
    ctx.stroke();
    ctx.restore();

    // 3. Compute coordinates for 99 perimeter beads
    const hStraight = (right - left) - 2 * cr; // 212
    const vStraight = (bottom - top) - 2 * cr; // 212
    const arcLen = 0.5 * Math.PI * cr; // 72.25
    const totalPerim = 2 * hStraight + 2 * vStraight + 4 * arcLen; // ~713
    const step = totalPerim / 99;

    function getPointOnRoundedRect(d) {
      let dist = d % totalPerim;
      if (dist < 0) dist += totalPerim;

      const halfH = hStraight / 2; // 106
      if (dist <= halfH) {
        return { x: 180 + dist, y: top };
      }
      dist -= halfH;

      if (dist <= arcLen) {
        const theta = -Math.PI / 2 + (dist / arcLen) * (Math.PI / 2);
        return { x: right - cr + Math.cos(theta) * cr, y: top + cr + Math.sin(theta) * cr };
      }
      dist -= arcLen;

      if (dist <= vStraight) {
        return { x: right, y: top + cr + dist };
      }
      dist -= vStraight;

      if (dist <= arcLen) {
        const theta = 0 + (dist / arcLen) * (Math.PI / 2);
        return { x: right - cr + Math.cos(theta) * cr, y: bottom - cr + Math.sin(theta) * cr };
      }
      dist -= arcLen;

      if (dist <= hStraight) {
        return { x: right - cr - dist, y: bottom };
      }
      dist -= hStraight;

      if (dist <= arcLen) {
        const theta = Math.PI / 2 + (dist / arcLen) * (Math.PI / 2);
        return { x: left + cr + Math.cos(theta) * cr, y: bottom - cr + Math.sin(theta) * cr };
      }
      dist -= arcLen;

      if (dist <= vStraight) {
        return { x: left, y: bottom - cr - dist };
      }
      dist -= vStraight;

      if (dist <= arcLen) {
        const theta = Math.PI + (dist / arcLen) * (Math.PI / 2);
        return { x: left + cr + Math.cos(theta) * cr, y: top + cr + Math.sin(theta) * cr };
      }
      dist -= arcLen;

      return { x: left + cr + dist, y: top };
    }

    const currentInCycle = count % 100;

    // Draw Dangling Minaret Bead 0
    ctx.save();
    ctx.beginPath();
    ctx.arc(danglerX, danglerY, 13, 0, Math.PI * 2);
    const dGrad = ctx.createRadialGradient(danglerX - 3, danglerY - 3, 2, danglerX, danglerY, 13);
    dGrad.addColorStop(0, pal.dangler.c0);
    dGrad.addColorStop(0.5, pal.dangler.c1);
    dGrad.addColorStop(1, pal.dangler.c2);
    ctx.fillStyle = dGrad;
    ctx.shadowColor = pal.active.shadow;
    ctx.shadowBlur = 10;
    ctx.fill();
    ctx.strokeStyle = '#FFFFFF';
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.restore();

    // Draw 99 perimeter beads
    for (let i = 1; i < 100; i++) {
      const p = getPointOnRoundedRect((i - 1) * step + animOffset * 2.5);
      const isCounted = i <= currentInCycle;
      const isCurrent = i === currentInCycle;

      ctx.save();
      ctx.beginPath();
      const bRadius = isCurrent ? 8.5 : 6;
      ctx.arc(p.x, p.y, bRadius, 0, Math.PI * 2);

      if (isCounted) {
        ctx.fillStyle = pal.mode100Counted;
        ctx.shadowColor = pal.counted.shadow;
        ctx.shadowBlur = 6;
      } else if (isCurrent) {
        ctx.fillStyle = pal.mode100Active;
        ctx.shadowColor = pal.active.shadow;
        ctx.shadowBlur = 10;
      } else {
        ctx.fillStyle = pal.mode100Idle;
      }
      ctx.fill();
      ctx.strokeStyle = pal.idle.stroke;
      ctx.lineWidth = 1;
      ctx.stroke();
      ctx.restore();
    }
  }

  function renderTasbeeh() {
    const curZikr = getCurrentZikr();
    const count = getCount(curZikr.id);

    // Update active zikr header (if element exists)
    if (activePhraseEl) activePhraseEl.textContent = curZikr.phrase;
    if (activeVirtueEl) activeVirtueEl.textContent = curZikr.virtue;
    if (activeProofTextEl) activeProofTextEl.textContent = curZikr.proof;

    // Update the inner phrase inside the Misbaha beads
    if (tasbeehInnerPhrase) tasbeehInnerPhrase.textContent = curZikr.phrase;

    // Update floating hover tooltip popover
    if (tooltipZikrTitle) tooltipZikrTitle.textContent = curZikr.phrase;
    if (tooltipZikrVirtue) tooltipZikrVirtue.textContent = curZikr.virtue;
    if (tooltipZikrProof) tooltipZikrProof.textContent = curZikr.proof;

    // Update mode cards UI
    if (cardMode33 && cardMode100) {
      if (currentMode === '33') {
        cardMode33.classList.add('active');
        cardMode100.classList.remove('active');
        if (indicatorMode33) indicatorMode33.textContent = '✓ النمط الفعّال';
        if (indicatorMode100) indicatorMode100.textContent = 'اضغط للتفعيل';
        if (beadsHundredsBadge) beadsHundredsBadge.style.display = 'none';
      } else {
        cardMode100.classList.add('active');
        cardMode33.classList.remove('active');
        if (indicatorMode100) indicatorMode100.textContent = '✓ النمط الفعّال';
        if (indicatorMode33) indicatorMode33.textContent = 'اضغط للتفعيل';
        if (beadsHundredsBadge) {
          beadsHundredsBadge.style.display = 'block';
          beadsHundredsBadge.textContent = `مئات الدورات: ${toArDigits(hundredsCount)}`;
        }
      }
    }

    // Update goal pills
    goalPills.forEach(pill => {
      const g = parseInt(pill.getAttribute('data-goal'), 10);
      pill.classList.toggle('active', g === currentGoal);
    });

    // Update center count display
    if (beadsCenterCount) beadsCenterCount.textContent = count;
    if (beadsCenterTarget) {
      beadsCenterTarget.textContent = currentGoal > 0 ? `الهدف: ${toArDigits(currentGoal)}` : 'تسبيح حر ∞';
    }

    // Render Canvas
    if (currentMode === '33') {
      drawBeads33(count);
    } else {
      drawBeads100(count);
    }

    // Render Symmetrical Side Cards (Right & Left Columns)
    renderSidebarCards();
  }

  window.renderTasbeehEngine = renderTasbeeh;

  function createCardElement(z, idx) {
    const card = document.createElement('div');
    card.className = `zikr-card-item ${idx === activeIndex ? 'active' : ''}`;
    const cnt = getCount(z.id);

    card.innerHTML = `
      <div class="zikr-card-header">
        <div class="zikr-card-phrase">${z.phrase}</div>
        <span class="zikr-card-goal-chip">${toArDigits(z.goal || 33)} خرزة</span>
      </div>
      <div class="zikr-card-virtue">${z.virtue}</div>
      <div class="zikr-card-footer">
        <span class="zikr-card-counter-tag">📿 أُنجِز: ${toArDigits(cnt)} مرة</span>
        <span class="zikr-card-hadith-tag">${z.id === 'istighfar' ? 'سورة نوح ومسلم' : (z.id.includes('subhanallah') ? 'متفق عليه ومسلم' : 'حديث صحيح')}</span>
      </div>
    `;

    card.title = `${z.phrase}\n\n🌟 الفضيلة: ${z.virtue}\n\n📖 الدليل: ${z.proof}`;

    card.addEventListener('click', () => {
      selectZikr(idx);
    });

    return card;
  }

  function renderSidebarCards() {
    if (sideRightContainer) sideRightContainer.innerHTML = '';
    if (sideLeftContainer) sideLeftContainer.innerHTML = '';
    if (cardsContainer) cardsContainer.innerHTML = '';

    if (zikrListCountBadge) {
      zikrListCountBadge.textContent = `${toArDigits(allZikrs.length)} أذكار مباركة`;
    }

    allZikrs.forEach((z, idx) => {
      const card = createCardElement(z, idx);

      // Distribute symmetrically:
      // First 5 cards (0..4) go to Right Column: أذكار التنزيه والحمد
      // Next cards (5..end) go to Left Column: أذكار الاستعانة والتوكل والأذكار المخصصة
      if (idx < 5) {
        if (sideRightContainer) {
          sideRightContainer.appendChild(card);
        } else if (cardsContainer) {
          cardsContainer.appendChild(card);
        }
      } else {
        if (sideLeftContainer) {
          sideLeftContainer.appendChild(card);
        } else if (cardsContainer) {
          cardsContainer.appendChild(card);
        }
      }
    });
  }

  function selectZikr(idx) {
    if (idx < 0 || idx >= allZikrs.length) return;
    activeIndex = idx;
    localStorage.setItem('sb_active_zikr_index', activeIndex);
    const z = allZikrs[activeIndex];
    currentMode = z.mode || (z.goal === 100 ? '100' : '33');
    currentGoal = z.goal || (currentMode === '100' ? 100 : 33);
    renderTasbeeh();
  }

  // --- Increment Counter ---
  function onCountIncrement() {
    const curZikr = getCurrentZikr();
    let count = getCount(curZikr.id) + 1;
    animOffset += 1;

    let reachedGoal = false;

    if (currentMode === '100') {
      if (count > 100) {
        count = 1;
        hundredsCount++;
        localStorage.setItem('sb_tasbeeh_hundreds', hundredsCount);
        reachedGoal = true;
      } else if (count === 100) {
        reachedGoal = true;
      }
    } else {
      if (currentGoal > 0 && count % currentGoal === 0) {
        reachedGoal = true;
      }
    }

    setCount(curZikr.id, count);

    if (reachedGoal) {
      audioEngine.playGoalChime();
      if (window.electronAPI && window.electronAPI.sendNotification) {
        window.electronAPI.sendNotification('اكتملت دورة التسبيح المباركة!', `أتممت ${toArDigits(currentGoal)} مرة من ذكر "${curZikr.phrase}" تقبل الله طاعتكم`);
      }
    } else {
      audioEngine.playBeadClick();
    }

    renderTasbeeh();
  }

  // --- Event Listeners ---
  if (btnTapTasbeeh) btnTapTasbeeh.addEventListener('click', onCountIncrement);
  if (beadsCanvasWrapper) beadsCanvasWrapper.addEventListener('click', onCountIncrement);

  // Hover and Tap Tooltip for Inner Phrase (الفوائد والإسناد عند وضع المؤشر)
  if (tasbeehInnerPhraseWrap && tasbeehHoverTooltip) {
    tasbeehInnerPhraseWrap.addEventListener('mouseenter', () => {
      tasbeehHoverTooltip.classList.add('visible');
    });
    tasbeehInnerPhraseWrap.addEventListener('mouseleave', () => {
      tasbeehHoverTooltip.classList.remove('visible');
    });
    tasbeehInnerPhraseWrap.addEventListener('click', (e) => {
      e.stopPropagation(); // Do not increment counter when clicking the text
      tasbeehHoverTooltip.classList.toggle('visible');
    });

    document.addEventListener('click', (e) => {
      if (!tasbeehInnerPhraseWrap.contains(e.target)) {
        tasbeehHoverTooltip.classList.remove('visible');
      }
    });
  }

  // Keyboard Spacebar for Tasbeeh
  window.addEventListener('keydown', (e) => {
    if (e.code === 'Space' && state.activeTab === 'tasbeeh') {
      const tag = e.target.tagName;
      if (tag !== 'INPUT' && tag !== 'TEXTAREA') {
        e.preventDefault();
        onCountIncrement();
      }
    }
  });

  // Reset counter
  if (btnResetTasbeeh) {
    btnResetTasbeeh.addEventListener('click', () => {
      const curZikr = getCurrentZikr();
      setCount(curZikr.id, 0);
      animOffset = 0;
      if (currentMode === '100') {
        hundredsCount = 0;
        localStorage.setItem('sb_tasbeeh_hundreds', 0);
      }
      renderTasbeeh();
    });
  }

  // Switch to Mode 33 Card
  if (cardMode33) {
    cardMode33.addEventListener('click', () => {
      currentMode = '33';
      currentGoal = 33;
      renderTasbeeh();
    });
  }

  // Switch to Mode 100 Card
  if (cardMode100) {
    cardMode100.addEventListener('click', () => {
      currentMode = '100';
      currentGoal = 100;
      renderTasbeeh();
    });
  }

  // Quick Goal Pills
  goalPills.forEach(pill => {
    pill.addEventListener('click', () => {
      const g = parseInt(pill.getAttribute('data-goal'), 10);
      currentGoal = g;
      if (g === 100) currentMode = '100';
      else if (g === 33) currentMode = '33';
      renderTasbeeh();
    });
  });

  // Navigation between Zikrs (< >)
  if (btnPrevZikr) {
    btnPrevZikr.addEventListener('click', () => {
      const newIdx = activeIndex === 0 ? allZikrs.length - 1 : activeIndex - 1;
      selectZikr(newIdx);
    });
  }

  if (btnNextZikr) {
    btnNextZikr.addEventListener('click', () => {
      const newIdx = activeIndex === allZikrs.length - 1 ? 0 : activeIndex + 1;
      selectZikr(newIdx);
    });
  }

  // Sound Toggle
  if (btnToggleTasbeehSound) {
    btnToggleTasbeehSound.addEventListener('click', () => {
      audioEngine.enabled = !audioEngine.enabled;
      if (tasbeehSoundIcon) tasbeehSoundIcon.textContent = audioEngine.enabled ? '🔊' : '🔇';
      if (tasbeehSoundLabel) tasbeehSoundLabel.textContent = audioEngine.enabled ? 'صوت الخرز: مفعل' : 'صوت الخرز: مكتوم';
    });
  }

  // Custom Zikr Modal
  if (btnOpenAddZikrModal) {
    btnOpenAddZikrModal.addEventListener('click', () => {
      if (modalAddCustomZikr) modalAddCustomZikr.classList.add('open');
      if (inputCustomZikrText) {
        inputCustomZikrText.value = '';
        inputCustomZikrText.focus();
      }
    });
  }

  function closeAddZikrModal() {
    if (modalAddCustomZikr) modalAddCustomZikr.classList.remove('open');
  }

  if (btnCloseAddZikrModal) btnCloseAddZikrModal.addEventListener('click', closeAddZikrModal);
  if (btnCancelAddZikr) btnCancelAddZikr.addEventListener('click', closeAddZikrModal);

  if (btnConfirmAddZikr) {
    btnConfirmAddZikr.addEventListener('click', () => {
      const text = (inputCustomZikrText.value || '').trim();
      if (!text) {
        alert('يرجى إدخال نص الذكر الشريف أو الدعاء');
        return;
      }

      const virtue = (inputCustomZikrVirtue.value || '').trim() || 'ذكر ودعاء مخصص مبارك';
      const goalRadio = document.querySelector('input[name="customGoalRadio"]:checked');
      const goalVal = goalRadio ? parseInt(goalRadio.value, 10) : 33;
      const modeVal = goalVal === 100 ? '100' : '33';

      const newZikr = {
        id: `custom_${Date.now()}`,
        phrase: text,
        virtue: virtue,
        proof: 'ذكر ودعاء مبارك من اختيار المستخدم، والأصل في الذكر الإكثار عملاً بقوله تعالى: ﴿اذْكُرُوا اللَّهَ ذِكْرًا كَثِيرًا﴾.',
        goal: goalVal,
        mode: modeVal
      };

      customZikrs.push(newZikr);
      try {
        localStorage.setItem('sb_custom_zikrs', JSON.stringify(customZikrs));
      } catch (e) {}

      allZikrs = [...defaultZikrs, ...customZikrs];
      closeAddZikrModal();
      selectZikr(allZikrs.length - 1);
    });
  }

  // Initial render
  renderTasbeeh();
}

// =========================================================================
// --- 8. Continuous Mushaf Roll Engine (المصحف الشريف كرولة عمودية متصلة) ---
// طِبق الأصل 100% من تطبيق أندرويد (activity_main.xml, item_verse.xml, QuranAdapter.kt)
// =========================================================================

function toArabicDigits(num) {
  const arabicDigits = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'];
  return String(num).replace(/[0-9]/g, d => arabicDigits[parseInt(d, 10)]);
}

function normalizeArabicText(text) {
  if (!text) return '';
  return text
    .replace(/[\u064B-\u065F\u0670\u06D6-\u06ED\u0640]/g, '') // remove harakat, dagger alif, signs, tatweel
    .replace(/[أإآٱ]/g, 'ا')
    .replace(/[\u064A\u0649\u06CC]/g, 'ي')
    .replace(/[ة]/g, 'ه')
    .replace(/[^ا-ي0-9\s]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

function setupQuranRoll() {
  const container = document.getElementById('quranRollContainer');
  const scrollArea = document.getElementById('quranScrollArea');
  if (!container || !scrollArea) return;

  if (!window.QURAN_PAGES || !window.QURAN_SURAHS) {
    container.innerHTML = '<div style="text-align: center; padding: 50px; color: red;">قاعدة بيانات المصحف الشريف غير متوفرة</div>';
    return;
  }

  container.innerHTML = '';

  // Retrieve saved progress without wiping it!
  const savedPage = Math.max(1, Math.min(604, parseInt(localStorage.getItem('sb_last_page_num') || '1', 10)));
  const savedVerse = parseInt(localStorage.getItem('sb_last_verse_num') || '1', 10);
  state.quran.currentPageNum = savedPage;
  state.quran.isRestoringProgress = false;

  // Create 604 page placeholders with realistic 850px page height to prevent layout drift
  const totalPages = 604;
  for (let p = 1; p <= totalPages; p++) {
    const pageBlock = document.createElement('div');
    pageBlock.className = 'page-mushaf-block';
    pageBlock.id = `page-block-${p}`;
    pageBlock.setAttribute('data-page', p);
    pageBlock.style.minHeight = '820px';
    container.appendChild(pageBlock);
  }

  // 1. IntersectionObserver exclusively for On-Demand Virtual Rendering (no top-bar or progress interference)
  const renderObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const pageEl = entry.target;
        const p = parseInt(pageEl.getAttribute('data-page'), 10);
        renderPageBlockContent(pageEl, p);
      }
    });
  }, {
    root: scrollArea,
    rootMargin: '1600px 0px 1600px 0px'
  });

  // Observe all 604 blocks for lazy content population
  for (let p = 1; p <= totalPages; p++) {
    const el = document.getElementById(`page-block-${p}`);
    if (el) renderObserver.observe(el);
  }

  // Immediately render the saved page and adjacent pages
  for (let p = Math.max(1, savedPage - 2); p <= Math.min(604, savedPage + 2); p++) {
    const el = document.getElementById(`page-block-${p}`);
    if (el) renderPageBlockContent(el, p);
  }
  if (savedPage > 2) {
    for (let p = 1; p <= 2; p++) {
      const el = document.getElementById(`page-block-${p}`);
      if (el) renderPageBlockContent(el, p);
    }
  }

  // Initialize Supporting Modules
  setupQuranSessionTimer();
  setupUnifiedIndexDialog();
  setupVerseOptionsDialog();
  setupFontSettingsDialog();
  setupQuranSearchDialog();
  setupReciterSelectionDialog();
  setupQuranResumeButton();
  setupQuranReviewMode();
  setupFloatingAudioBar();
  setupVoiceSpeechTracker();
  setupHifzTutorDialog();

  // Set initial top bar to the SAVED PAGE without overwriting progress
  updateQuranTopBar(savedPage);

  // Single deterministic source of truth for Quran reading line: tracks both active page AND active verse
  function checkActiveQuranPageFromReadingLine() {
    if (state.quran.isRestoringProgress || state.activeTab !== 'quran') return;
    const sRect = scrollArea.getBoundingClientRect();
    if (sRect.height === 0) return;
    const readingY = sRect.top + 80; // Exactly below sticky top bars

    // Fast local bounded search around estimated page
    const approxPage = Math.max(1, Math.min(604, Math.floor(scrollArea.scrollTop / 820) + 1));
    const start = Math.max(1, approxPage - 3);
    const end = Math.min(604, approxPage + 4);

    let foundPage = -1;
    let activePageBlock = null;
    for (let p = start; p <= end; p++) {
      const b = document.getElementById(`page-block-${p}`);
      if (b) {
        const bRect = b.getBoundingClientRect();
        if (bRect.top <= readingY && bRect.bottom > readingY) {
          foundPage = p;
          activePageBlock = b;
          break;
        }
      }
    }

    // Fallback across full container if needed
    if (foundPage === -1) {
      const blocks = container.children;
      for (let i = 0; i < blocks.length; i++) {
        const b = blocks[i];
        const bRect = b.getBoundingClientRect();
        if (bRect.top <= readingY && bRect.bottom > readingY) {
          foundPage = parseInt(b.getAttribute('data-page'), 10) || (i + 1);
          activePageBlock = b;
          break;
        }
      }
    }

    if (foundPage <= 0 || !activePageBlock) return;

    // Track the active verse in real-time within activePageBlock
    let activeVerseEl = null;
    let activeAya = 1;
    let activeSura = null;
    let activeSuraName = '';

    const verses = activePageBlock.querySelectorAll('.verse-unit');
    if (verses.length > 0) {
      for (let i = 0; i < verses.length; i++) {
        const v = verses[i];
        const vRect = v.getBoundingClientRect();
        if (vRect.top <= readingY + 35 && vRect.bottom >= readingY - 35) {
          activeVerseEl = v;
          break;
        } else if (vRect.bottom >= readingY) {
          activeVerseEl = v;
          break;
        }
      }
      if (!activeVerseEl) {
        activeVerseEl = verses[verses.length - 1];
      }
    }

    if (activeVerseEl) {
      activeAya = parseInt(activeVerseEl.getAttribute('data-aya'), 10) || 1;
      activeSura = parseInt(activeVerseEl.getAttribute('data-sura'), 10);
      activeSuraName = activeVerseEl.getAttribute('data-sura-name') || '';
    } else {
      const pageAyahs = window.QURAN_PAGES ? window.QURAN_PAGES[String(foundPage)] : null;
      const first = pageAyahs && pageAyahs.length ? pageAyahs[0] : null;
      if (first) {
        activeAya = first.aya || 1;
        activeSura = first.sura;
        activeSuraName = first.sura_name;
      }
    }

    // Always update Quran top bar dynamically with surah, ayah, and page
    updateQuranTopBar(foundPage, { aya: activeAya, sura: activeSura, sura_name: activeSuraName });

    // Debounce saving progress to localStorage (350ms of stable reading)
    clearTimeout(state.quran._saveProgressTimer);
    state.quran._saveProgressTimer = setTimeout(() => {
      saveQuranProgress(foundPage, activeSuraName, activeAya, false);
    }, 350);
  }

  let scrollSaveDebounce = null;
  scrollArea.addEventListener('scroll', () => {
    if (state.quran.isRestoringProgress || state.activeTab !== 'quran') return;
    clearTimeout(scrollSaveDebounce);
    scrollSaveDebounce = setTimeout(checkActiveQuranPageFromReadingLine, 40);
  }, { passive: true });
}

function renderPageBlockContent(pageEl, pageNum) {
  if (pageEl.dataset.rendered === 'true') return;
  pageEl.dataset.rendered = 'true';
  pageEl.style.minHeight = '820px';

  const pageAyahs = window.QURAN_PAGES[String(pageNum)] || [];
  if (pageAyahs.length === 0) return;

  let currentSurahIdOnPage = -1;
  let currentVersesContainer = null;

  pageAyahs.forEach((a) => {
    // A surah header is ONLY displayed if this is the start of that surah (aya 1 or basmalah aya 0)
    const isNewSurah = (a.aya === 1) || (a.isBasmala && a.aya === 0);

    if (isNewSurah && a.sura !== currentSurahIdOnPage) {
      currentSurahIdOnPage = a.sura;
      const surahMeta = window.QURAN_SURAHS.find(s => s.id === a.sura) || {
        name: a.sura_name,
        type: a.revelation,
        ayah_count: 0
      };
      const suraType = surahMeta.type === 'Makkiyah' ? 'مَكِّيَّة' : 'مَدَنِيَّة';

      // 1. Ornate Header Card (basmalahCard in item_verse.xml)
      const headerCard = document.createElement('div');
      headerCard.className = 'sura-header-card';
      headerCard.setAttribute('data-surah', a.sura);
      headerCard.id = `surah-header-${a.sura}`;
      headerCard.innerHTML = `
        <div class="sura-meta-left">${suraType} • ${surahMeta.ayah_count} آية</div>
        <div class="sura-name-right">۞ سُورَةُ ${surahMeta.name} ۞</div>
      `;
      pageEl.appendChild(headerCard);

      // 2. Basmalah Line (Except Surah 9 At-Tawbah & Surah 1 Al-Fatiha where it is verse 1)
      if (a.sura !== 9 && a.sura !== 1) {
        const basmalahLine = document.createElement('div');
        basmalahLine.className = 'sura-basmalah-line';
        basmalahLine.textContent = 'بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ';
        pageEl.appendChild(basmalahLine);
      }

      // New verses block for this surah
      currentVersesContainer = document.createElement('div');
      currentVersesContainer.className = 'page-verses-text';
      if (state.quran.fontSize) {
        currentVersesContainer.style.fontSize = `${state.quran.fontSize}px`;
      }
      if (state.quran.fontFamily) {
        currentVersesContainer.style.fontFamily = `var(--font-quran), '${state.quran.fontFamily}', serif`;
      }
      pageEl.appendChild(currentVersesContainer);
    }

    if (!currentVersesContainer) {
      currentVersesContainer = document.createElement('div');
      currentVersesContainer.className = 'page-verses-text';
      if (state.quran.fontSize) {
        currentVersesContainer.style.fontSize = `${state.quran.fontSize}px`;
      }
      pageEl.appendChild(currentVersesContainer);
    }

    // Skip separate Basmalah entry if aya === 0
    if (a.isBasmala && a.aya === 0) return;

    // Clean verse text (strip duplicate Basmalah if Surah 1)
    let verseText = a.text;
    if (a.sura === 1 && a.aya === 1) {
      // Verse 1 of Fatiha is the Basmalah itself
      verseText = 'بِسۡمِ ٱللَّهِ ٱلرَّحۡمَـٰنِ ٱلرَّحِیمِ';
    } else {
      verseText = verseText.replace(/^(بِسۡ?مِ\s+ٱللَّهِ\s+ٱلرَّحۡ?مَـٰنِ\s+ٱلرَّحِیمِ|بِسْمِ\s+اللَّهِ\s+الرَّحْمَٰنِ\s+الرَّحِيمِ)\s*/, '');
    }
    // Remove existing end glyph if present to ensure unified formatting
    verseText = verseText.replace(/[۝\u06DD][\s\S]*$/, '').trim();

    // Create Verse Unit Span
    const span = document.createElement('span');
    span.className = 'verse-unit';
    span.id = `verse-${a.sura}-${a.aya}`;
    span.setAttribute('data-sura', a.sura);
    span.setAttribute('data-aya', a.aya);
    span.setAttribute('data-page', pageNum);
    span.setAttribute('data-sura-name', a.sura_name);

    span.innerHTML = `${verseText} <span class="verse-symbol-glyph">۝${toArabicDigits(a.aya)}</span> `;

    span.addEventListener('click', (e) => {
      e.stopPropagation();
      openVerseOptions(a, pageNum, verseText);
    });

    currentVersesContainer.appendChild(span);

    // If inline translation active, append line
    if (state.quran.translationLang === 'en' && a.translation_en) {
      const transLine = document.createElement('div');
      transLine.className = 'verse-trans-line';
      transLine.setAttribute('data-trans-aya', `${a.sura}-${a.aya}`);
      transLine.textContent = `[${a.sura}:${a.aya}] ${a.translation_en}`;
      currentVersesContainer.appendChild(transLine);
    } else if (state.quran.translationLang === 'id' && a.translation_id) {
      const transLine = document.createElement('div');
      transLine.className = 'verse-trans-line';
      transLine.setAttribute('data-trans-aya', `${a.sura}-${a.aya}`);
      transLine.textContent = `[${a.sura}:${a.aya}] ${a.translation_id}`;
      currentVersesContainer.appendChild(transLine);
    }
  });

  // 3. Page Indicator Footer (ص 1 مع الدوائر الجانبية كما في QuranAdapter.kt)
  const footer = document.createElement('div');
  footer.className = 'page-indicator-footer';

  const dotLeft = document.createElement('div');
  dotLeft.className = `indicator-dot ${pageNum >= 3 && pageNum % 2 === 0 ? 'red' : ''}`;
  dotLeft.style.visibility = (pageNum >= 3 && pageNum % 2 === 0) ? 'visible' : 'hidden';

  const centerLabel = document.createElement('div');
  centerLabel.className = 'page-center-label';
  centerLabel.textContent = `صـ ${pageNum} • الجزء ${pageAyahs[0]?.juz || 1}`;

  const dotRight = document.createElement('div');
  dotRight.className = `indicator-dot ${pageNum >= 3 && pageNum % 2 !== 0 ? 'green' : ''}`;
  dotRight.style.visibility = (pageNum >= 3 && pageNum % 2 !== 0) ? 'visible' : 'hidden';

  footer.appendChild(dotLeft);
  footer.appendChild(centerLabel);
  footer.appendChild(dotRight);
  pageEl.appendChild(footer);

  // 4. If Page 604 (End of Quran): Append Khatm Al-Quran Card (دعاء ختم القرآن)
  if (pageNum === 604) {
    const khatmCard = document.createElement('div');
    khatmCard.className = 'khatm-quran-card';
    khatmCard.innerHTML = `
      <div class="khatm-title">۞ دُعَاءُ خَتْمِ الْقُرْآنِ الْكَرِيمِ ۞</div>
      <div class="khatm-divider"></div>
      <div class="khatm-body">
        اللَّهُمَّ ارْحَمْنِي بِالْقُرْآنِ، وَاجْعَلْهُ لِي إِمَامًا وَنُورًا وَهُدًى وَرَحْمَةً.<br><br>
        اللَّهُمَّ ذَكِّرْنِي مِنْهُ مَا نَسِيتُ، وَعَلِّمْنِي مِنْهُ مَا جَهِلْتُ، وَارْزُقْنِي تِلاَوَتَهُ آنَاءَ اللَّيْلِ وَأَطْرَافَ النَّهَارِ، وَاجْعَلْهُ لِي حُجَّةً يَا رَبَّ الْعَالَمِينَ.<br><br>
        اللَّهُمَّ أَصْلِحْ لِي دِينِي الَّذِي هُوَ عِصْمَةُ أَمْرِي، وَأَصْلِحْ لِي دُنْيَايَ الَّتِي فِيهَا مَعَاشِي، وَأَصْلِحْ لِي آخِرَتِي الَّتِي فِيهَا مَعَادِي، وَاجْعَلِ الْحَيَاةَ زِيَادَةً لِي فِي كُلِّ خَيْرٍ، وَاجْعَلِ الْمَوْتَ رَاحَةً لِي مِنْ كُلِّ شَرٍّ.<br><br>
        اللَّهُمَّ اجْعَلْ خَيْرَ عُمْرِي آخِرَهُ، وَخَيْرَ عَمَلِي خَوَاتِمَهُ، وَخَيْرَ أَيَّامِي يَوْمَ أَلْقَاكَ فِيهِ.<br><br>
        اللَّهُمَّ إِنِّي أَسْأَلُكَ عِيشَةً هَنِيَّةً، وَمِيتَةً سَوِيَّةً، وَمَرَدًّا غَيْرَ مُخْزٍ وَلاَ فَاضِحٍ.<br><br>
        اللَّهُمَّ أَحْسِنْ عَاقِبَتَنَا فِي الأُمُورِ كُلِّهَا، وَأَجِرْنَا مِنْ خِزْيِ الدُّنْيَا وَعَذَابِ الآخِرَةِ.<br><br>
        وَصَلَّى اللَّهُ عَلَى سَيِّدِنَا مُحَمَّدٍ وَعَلَى آلِهِ وَصَحْبِهِ وَسَلَّمَ تَسْلِيمًا كَثِيرًا.
      </div>
    `;
    pageEl.appendChild(khatmCard);
  }
  // Restore visual bookmark ribbon if this page matches saved position
  const savedP = parseInt(localStorage.getItem('sb_last_page_num') || '1', 10);
  if (pageNum === savedP) {
    const savedV = parseInt(localStorage.getItem('sb_last_verse_num') || '1', 10);
    setTimeout(() => {
      if (typeof updateBookmarkVisualRibbon === 'function') {
        updateBookmarkVisualRibbon(pageNum, savedV);
      }
    }, 15);
  }
}

function getManzilForPage(pageNum) {
  if (pageNum < 106) return 1;
  if (pageNum < 208) return 2;
  if (pageNum < 282) return 3;
  if (pageNum < 367) return 4;
  if (pageNum < 446) return 5;
  if (pageNum < 518) return 6;
  return 7;
}

// Visual Bookmark Highlighting Helper (تظليل أنيق ونظيف دون إضافة نصوص داخل الآية)
function updateBookmarkVisualRibbon(pageNum, verseNum) {
  document.querySelectorAll('.is-bookmarked-position').forEach(el => el.classList.remove('is-bookmarked-position'));
  document.querySelectorAll('.bookmark-ribbon-tag').forEach(el => el.remove());

  const p = pageNum || parseInt(localStorage.getItem('sb_last_page_num') || '1', 10);
  const v = verseNum || parseInt(localStorage.getItem('sb_last_verse_num') || '1', 10);

  const pageBlock = document.getElementById(`page-block-${p}`);
  if (!pageBlock) return;

  const verseEl = pageBlock.querySelector(`.verse-unit[data-aya="${v}"]`) ||
                  pageBlock.querySelector('.verse-unit');
  if (verseEl) {
    verseEl.classList.add('is-bookmarked-position');
    verseEl.title = `موضع القراءة المحفوظ (الآية ${toArabicDigits(v)})`;
  }
}

function saveQuranProgress(pageNum, suraName, verseNum, force = false) {
  if (!force && state.quran.isRestoringProgress) return;
  if (!pageNum || pageNum < 1 || pageNum > 604) return;

  const currentSaved = parseInt(localStorage.getItem('sb_last_page_num') || '1', 10);
  if (!force && pageNum === 1 && currentSaved > 1 && !state.quran.explicitUserNavigatedToFirst) {
    const scrollArea = document.getElementById('quranScrollArea');
    if (scrollArea && scrollArea.scrollTop > 300) {
      return;
    }
  }

  try {
    localStorage.setItem('sb_last_page_num', String(pageNum));
    if (suraName) localStorage.setItem('sb_last_surah_name', suraName);
    if (verseNum) localStorage.setItem('sb_last_verse_num', String(verseNum));
    localStorage.setItem('sb_last_timestamp', String(Date.now()));

    state.quran.currentPageNum = pageNum;
    if (verseNum) state.quran.currentVerseNum = verseNum;

    updateBookmarkVisualRibbon(pageNum, verseNum);

    if (typeof updateDynamicHeaderProgress === 'function') {
      updateDynamicHeaderProgress();
    }
  } catch (_) {}
}

// Update Top Bar on Scroll & Reading Tracking
function updateQuranTopBar(pageNum, activeVerseInfo = null) {
  state.quran.currentPageNum = pageNum;
  const pageAyahs = window.QURAN_PAGES ? window.QURAN_PAGES[String(pageNum)] || [] : [];
  const first = pageAyahs[0];

  const suraName = activeVerseInfo ? activeVerseInfo.sura_name : (first ? first.sura_name : '');
  const ayaNum = activeVerseInfo ? activeVerseInfo.aya : (first ? first.aya : 1);
  const suraId = activeVerseInfo ? activeVerseInfo.sura : (first ? first.sura : 1);
  const juzNum = first ? first.juz : 1;
  const manzilNum = first ? (first.manzil || getManzilForPage(pageNum)) : getManzilForPage(pageNum);

  state.quran.currentSurahId = suraId;
  state.quran.currentVerseNum = ayaNum;
  state.quran.currentJuzNum = juzNum;
  state.quran.currentManzilNum = manzilNum;

  const suraEl = document.getElementById('topBarSuraText');
  if (suraEl && suraName) {
    suraEl.textContent = `سورة ${suraName} (${toArabicDigits(ayaNum)})`;
  }

  const juzEl = document.getElementById('topBarJuzText');
  if (juzEl) juzEl.textContent = `الجزء ${toArabicDigits(juzNum)}`;

  const manzilEl = document.getElementById('topBarManzilText');
  if (manzilEl) manzilEl.textContent = `المنزل ${toArabicDigits(manzilNum)}`;

  const suraRukooEl = document.getElementById('tvSuraRukoo');
  if (suraRukooEl) suraRukooEl.textContent = `ع سورة ${suraId}`;

  const pageEl = document.getElementById('topBarPageText');
  if (pageEl) pageEl.textContent = `صـ ${toArabicDigits(pageNum)}`;

  // Rukoo Progress Bar (percentage of Quran read)
  const bar = document.getElementById('rukooProgressBar');
  if (bar) {
    const pct = ((pageNum / 604) * 100).toFixed(1);
    bar.style.width = `${pct}%`;
  }
}

// Jump directly to any page and optionally highlight a verse or align to a specific Surah
function jumpToQuranPage(pageNum, verseAyaNum = null, isSmooth = false, targetSuraId = null) {
  const p = Math.max(1, Math.min(604, pageNum));
  const pageBlock = document.getElementById(`page-block-${p}`);
  const scrollArea = document.getElementById('quranScrollArea');
  if (!pageBlock || !scrollArea) return;

  // Prevent scroll listeners from overwriting progress during jump
  state.quran.isRestoringProgress = true;

  // Ensure target page and adjacent pages are synchronously populated
  for (let i = Math.max(1, p - 2); i <= Math.min(604, p + 2); i++) {
    const b = document.getElementById(`page-block-${i}`);
    if (b) renderPageBlockContent(b, i);
  }

  // Pass 1: instant alignment of page block
  const pRect = pageBlock.getBoundingClientRect();
  const sRect = scrollArea.getBoundingClientRect();
  scrollArea.scrollTop += (pRect.top - sRect.top);

  // Pass 2: check if targetSuraId or verseAyaNum is specified, align right to its header card
  requestAnimationFrame(() => {
    let targetEl = pageBlock;
    let offsetAdjustment = 0;

    if (targetSuraId) {
      const suraHeader = pageBlock.querySelector(`.sura-header-card[data-surah="${targetSuraId}"]`) ||
                         document.querySelector(`.sura-header-card[data-surah="${targetSuraId}"]`) ||
                         document.getElementById(`surah-header-${targetSuraId}`);
      if (suraHeader) {
        targetEl = suraHeader;
        offsetAdjustment = 74; // below sticky top bars
        suraHeader.classList.add('surah-header-glow');
        setTimeout(() => suraHeader.classList.remove('surah-header-glow'), 3500);
      }
    } else if (verseAyaNum && verseAyaNum > 0) {
      const verseEl = pageBlock.querySelector(`[data-aya="${verseAyaNum}"]`) ||
                      document.getElementById(`verse-${state.quran.currentSurahId}-${verseAyaNum}`);
      if (verseEl) {
        targetEl = verseEl;
        offsetAdjustment = 80;
        document.querySelectorAll('.verse-unit.highlighted').forEach(el => el.classList.remove('highlighted'));
        verseEl.classList.add('highlighted');
        setTimeout(() => verseEl.classList.remove('highlighted'), 3500);
      }
    }

    const tRect = targetEl.getBoundingClientRect();
    const sRect2 = scrollArea.getBoundingClientRect();
    scrollArea.scrollTop += (tRect.top - sRect2.top) - offsetAdjustment;

    // Pass 3 (subpixel/font settle confirmation)
    requestAnimationFrame(() => {
      const tRect3 = targetEl.getBoundingClientRect();
      const sRect3 = scrollArea.getBoundingClientRect();
      const delta = (tRect3.top - sRect3.top) - offsetAdjustment;
      if (Math.abs(delta) > 2) {
        scrollArea.scrollTop += delta;
      }

      // Save progress stably with FORCE = true
      const pageAyahs = window.QURAN_PAGES ? window.QURAN_PAGES[String(p)] : null;
      let targetAyahData = null;
      if (pageAyahs && pageAyahs.length) {
        if (verseAyaNum) {
          targetAyahData = pageAyahs.find(a => a.aya === verseAyaNum) || pageAyahs[0];
        } else {
          targetAyahData = pageAyahs[0];
        }
      }
      const suraName = targetAyahData ? targetAyahData.sura_name : '';
      const aya = targetAyahData ? targetAyahData.aya : (verseAyaNum || 1);
      const suraId = targetAyahData ? targetAyahData.sura : (targetSuraId || 1);

      updateQuranTopBar(p, { aya, sura: suraId, sura_name: suraName });
      saveQuranProgress(p, suraName, aya, true);
      updateBookmarkVisualRibbon(p, aya);

      // Release progress restore lock after settling
      setTimeout(() => {
        state.quran.isRestoringProgress = false;
      }, 500);
    });
  });
}

// Session Timer (tvSessionTimer)
function setupQuranSessionTimer() {
  const timerEl = document.getElementById('tvSessionTimer');
  const sessionRukooEl = document.getElementById('tvSessionRukoo');
  const totalRukooEl = document.getElementById('tvTotalRukoo');

  if (state.quran.sessionTimerInterval) {
    clearInterval(state.quran.sessionTimerInterval);
  }

  state.quran.sessionTimerInterval = setInterval(() => {
    state.quran.sessionTimerSeconds++;
    const totalSec = state.quran.sessionTimerSeconds;
    const h = Math.floor(totalSec / 3600);
    const m = Math.floor((totalSec % 3600) / 60);
    const s = totalSec % 60;

    const formatted = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    if (timerEl) timerEl.textContent = formatted;

    // Track approximate session rukoo
    if (sessionRukooEl) sessionRukooEl.textContent = `ع جلسة ${Math.floor(state.quran.currentPageNum / 2)}`;
    if (totalRukooEl) totalRukooEl.textContent = `ع كلي ${Math.floor(state.quran.currentPageNum / 2)}`;
  }, 1000);
}

// Unified Index Dialog (dialog_unified_index.xml)
function setupUnifiedIndexDialog() {
  const modal = document.getElementById('modalUnifiedIndex');
  const openBtn = document.getElementById('btn-open-index');
  const closeBtn = document.getElementById('btnCloseIndex');
  const grid = document.getElementById('indexContentGrid');
  const inputSearch = document.getElementById('inputIndexSearch');

  const tabSurahs = document.getElementById('idxTabSurahs');
  const tabJuz = document.getElementById('idxTabJuz');
  const tabManzil = document.getElementById('idxTabManzil');
  const tabPages = document.getElementById('idxTabPages');

  if (!modal || !grid) return;

  function closeModal() {
    modal.classList.remove('open');
  }

  function openModal() {
    modal.classList.add('open');
    if (inputSearch) inputSearch.value = '';
    renderSurahsTab();
  }

  if (openBtn) openBtn.addEventListener('click', openModal);
  if (closeBtn) closeBtn.addEventListener('click', closeModal);
  modal.addEventListener('click', (e) => {
    if (e.target === modal) closeModal();
  });

  function setActiveTab(btn) {
    [tabSurahs, tabJuz, tabManzil, tabPages].forEach(b => b?.classList.remove('active'));
    btn?.classList.add('active');
  }

  function navigateToQuranTarget(targetPage, suraName = '', suraId = null) {
    closeModal();
    const p = Math.max(1, Math.min(604, parseInt(targetPage, 10)));
    state.quran.explicitUserNavigatedToFirst = (p === 1);
    if (typeof window.switchTab === 'function') {
      window.switchTab('quran', true); // skipAutoRestore to preserve requested target
    }
    // Direct instant alignment to page and sura header on the first click
    jumpToQuranPage(p, null, false, suraId);
  }

  function renderSurahsTab(query = '') {
    setActiveTab(tabSurahs);
    grid.innerHTML = '';
    const qNorm = query.trim() ? normalizeArabicText(query.trim()) : '';

    let list = window.QURAN_SURAHS || [];
    if (qNorm) {
      list = list.filter(s => {
        const sNorm = normalizeArabicText(s.name || '');
        const idMatch = String(s.id) === query.trim();
        const pageMatch = String(s.page) === query.trim();
        return sNorm.includes(qNorm) || idMatch || pageMatch;
      });
    }

    if (list.length === 0) {
      grid.innerHTML = `<div style="grid-column: 1 / -1; text-align: center; padding: 24px; color: #64748B;">لم يتم العثور على سورة مطابقة لـ "${query}"</div>`;
      return;
    }

    list.forEach(s => {
      const card = document.createElement('div');
      card.className = 'index-item-card';
      card.innerHTML = `
        <div>
          <div class="index-item-title">${s.id}. سورة ${s.name}</div>
          <div style="font-size: 11px; color: #64748B;">${s.type === 'Makkiyah' ? 'مكية' : 'مدنية'} • ${s.ayah_count} آية</div>
        </div>
        <div class="index-item-page">صـ ${s.page}</div>
      `;
      card.addEventListener('click', () => {
        navigateToQuranTarget(s.page, s.name, s.id);
      });
      grid.appendChild(card);
    });
  }

  // Live Index Search input
  if (inputSearch) {
    inputSearch.addEventListener('input', (e) => {
      renderSurahsTab(e.target.value);
    });
  }

  function renderJuzTab() {
    setActiveTab(tabJuz);
    grid.innerHTML = '';
    // Map start pages for all 30 Juz
    const juzStarts = [
      1, 22, 42, 62, 82, 102, 122, 142, 162, 182,
      202, 222, 242, 262, 282, 302, 322, 342, 362, 382,
      402, 422, 442, 462, 482, 502, 522, 542, 562, 582
    ];
    juzStarts.forEach((page, idx) => {
      const jNum = idx + 1;
      const card = document.createElement('div');
      card.className = 'index-item-card';
      card.innerHTML = `
        <div class="index-item-title">الجزء ${jNum}</div>
        <div class="index-item-page">يبدأ من صـ ${page}</div>
      `;
      card.addEventListener('click', () => {
        navigateToQuranTarget(page, `الجزء ${jNum}`);
      });
      grid.appendChild(card);
    });
  }

  function renderManzilTab() {
    setActiveTab(tabManzil);
    grid.innerHTML = '';
    const manazilList = [
      { id: 1, name: 'المنزل الأول', range: 'من سورة الفاتحة إلى سورة النساء', page: 1 },
      { id: 2, name: 'المنزل الثاني', range: 'من سورة المائدة إلى سورة التوبة', page: 106 },
      { id: 3, name: 'المنزل الثالث', range: 'من سورة يونس إلى سورة النحل', page: 208 },
      { id: 4, name: 'المنزل الرابع', range: 'من سورة الإسراء إلى سورة الفرقان', page: 282 },
      { id: 5, name: 'المنزل الخامس', range: 'من سورة الشعراء إلى سورة يس', page: 367 },
      { id: 6, name: 'المنزل السادس', range: 'من سورة الصافات إلى سورة الحجرات', page: 446 },
      { id: 7, name: 'المنزل السابع', range: 'من سورة ق إلى سورة الناس', page: 518 }
    ];
    manazilList.forEach(m => {
      const card = document.createElement('div');
      card.className = 'index-item-card';
      card.innerHTML = `
        <div>
          <div class="index-item-title">${m.name}</div>
          <div style="font-size: 11.5px; color: #64748B;">${m.range}</div>
        </div>
        <div class="index-item-page">يبدأ من صـ ${m.page}</div>
      `;
      card.addEventListener('click', () => {
        navigateToQuranTarget(m.page, m.name);
      });
      grid.appendChild(card);
    });
  }

  function renderPagesTab() {
    setActiveTab(tabPages);
    grid.innerHTML = '';
    for (let p = 1; p <= 604; p++) {
      const card = document.createElement('div');
      card.className = 'index-item-card';
      card.style.justifyContent = 'center';
      card.style.padding = '8px 10px';
      card.innerHTML = `<span style="font-weight: bold; font-size: 13.5px; color: var(--text-primary);">ص ${p}</span>`;
      card.addEventListener('click', () => {
        navigateToQuranTarget(p, `صفحة ${p}`);
      });
      grid.appendChild(card);
    }
  }

  if (tabSurahs) tabSurahs.addEventListener('click', () => renderSurahsTab());
  if (tabJuz) tabJuz.addEventListener('click', renderJuzTab);
  if (tabManzil) tabManzil.addEventListener('click', renderManzilTab);
  if (tabPages) tabPages.addEventListener('click', renderPagesTab);
}

// Verse Options Bottom Sheet (dialog_verse_options.xml)
function setupVerseOptionsDialog() {
  const modal = document.getElementById('modalVerseOptions');
  const closeBtn = document.getElementById('btnCloseVerseModal');
  const playBtn = document.getElementById('btnOptPlayAudio');
  const tafsirBtn = document.getElementById('btnOptShowTafsir');
  const transBtn = document.getElementById('btnOptShowTranslation');
  const hifzBtn = document.getElementById('btnOptHifzRepeat');
  const copyBtn = document.getElementById('btnOptCopyVerse');
  const assistantBtn = document.getElementById('btnOptAskAssistant');
  const labelCopy = document.getElementById('labelCopyBtn');
  const tafsirBox = document.getElementById('tafsirContentArea');
  const transBox = document.getElementById('translationContentArea');

  if (!modal) return;

  function closeModal() {
    modal.classList.remove('open');
  }

  if (closeBtn) closeBtn.addEventListener('click', closeModal);
  modal.addEventListener('click', (e) => {
    if (e.target === modal) closeModal();
  });

  const bookmarkBtn = document.getElementById('btnOptBookmarkVerse');
  if (bookmarkBtn) {
    bookmarkBtn.addEventListener('click', () => {
      const v = state.quran.selectedVerse;
      if (!v) return;
      closeModal();
      const p = v.pageNum || state.quran.currentPageNum || 1;
      saveQuranProgress(p, v.sura_name, v.aya, true);
      updateBookmarkVisualRibbon(p, v.aya);
      if (typeof showAppToast === 'function') {
        showAppToast(`🔖 تم تثبيت فاصلة القراءة: سورة ${v.sura_name} (آية ${toArabicDigits(v.aya)}) - صـ ${toArabicDigits(p)}`, 'success');
      }
    });
  }

  // 1. Play Audio (Mishary Alafasy EveryAyah stream with auto-advance)
  if (playBtn) {
    playBtn.addEventListener('click', () => {
      const v = state.quran.selectedVerse;
      if (!v) return;
      state.quran.hifzCurrentIteration = 1;
      playAyahRecitation(v.sura, v.aya);
      closeModal();
    });
  }

  // 2. Show Tafsir
  if (tafsirBtn) {
    tafsirBtn.addEventListener('click', () => {
      const v = state.quran.selectedVerse;
      if (!v || !tafsirBox) return;
      if (tafsirBox.style.display === 'block') {
        tafsirBox.style.display = 'none';
      } else {
        tafsirBox.style.display = 'block';
        tafsirBox.innerHTML = `<strong>التفسير الميسر:</strong><br>${v.tafsir || 'التفسير الميسر غير متاح لهذه الآية حالياً.'}`;
      }
    });
  }

  // 3. Show Translation (English & Indonesian)
  if (transBtn) {
    transBtn.addEventListener('click', () => {
      const v = state.quran.selectedVerse;
      if (!v || !transBox) return;
      if (transBox.style.display === 'block') {
        transBox.style.display = 'none';
      } else {
        transBox.style.display = 'block';
        transBox.innerHTML = `
          <div style="margin-bottom: 12px;">
            <strong style="color: var(--text-primary); font-size: 13.5px;">🇬🇧 English (Saheeh International):</strong>
            <div dir="ltr" style="margin-top: 4px; color: #1E293B; line-height: 1.6; font-size: 14px;">${v.translation_en || 'English translation not available.'}</div>
          </div>
          <div style="border-top: 1px solid rgba(197,160,89,0.3); padding-top: 10px;">
            <strong style="color: var(--text-primary); font-size: 13.5px;">🇮🇩 Bahasa Indonesia:</strong>
            <div dir="ltr" style="margin-top: 4px; color: #1E293B; line-height: 1.6; font-size: 14px;">${v.translation_id || 'Terjemahan bahasa Indonesia tidak tersedia.'}</div>
          </div>
        `;
      }
    });
  }

  // 4. Hifz Repeat Mode
  if (hifzBtn) {
    hifzBtn.addEventListener('click', () => {
      const v = state.quran.selectedVerse;
      closeModal();
      const hifzModal = document.getElementById('modalHifzTutor');
      if (hifzModal) hifzModal.classList.add('open');
      if (v) {
        state.quran.hifzCurrentIteration = 1;
        playAyahRecitation(v.sura, v.aya);
      }
    });
  }

  // 5. Copy Verse
  if (copyBtn) {
    copyBtn.addEventListener('click', () => {
      const v = state.quran.selectedVerse;
      if (!v) return;
      const formatted = `${v.text} [سورة ${v.sura_name}: آية ${v.aya}]`;
      navigator.clipboard.writeText(formatted).then(() => {
        if (labelCopy) labelCopy.textContent = 'تم النسخ بنجاح!';
        setTimeout(() => {
          if (labelCopy) labelCopy.textContent = 'نسخ نص الآية';
        }, 1800);
      });
    });
  }

  // 6. Ask AI Assistant
  if (assistantBtn) {
    assistantBtn.addEventListener('click', () => {
      const v = state.quran.selectedVerse;
      if (!v) return;
      closeModal();
      if (window.switchTab) window.switchTab('assistant');
      const input = document.getElementById('assistantInput');
      if (input) {
        input.value = `اشرح لي تدبر الآية الكريمة: "${v.text}" [سورة ${v.sura_name}: آية ${v.aya}] واستنباط معانيها الإيمانية والعقدية وفق مذهب أهل السنة والجماعة وتفسير السعدي وابن كثير.`;
        setTimeout(() => {
          if (window.sendAssistantUserMessage) {
            window.sendAssistantUserMessage();
          } else {
            const sendBtn = document.getElementById('btnSendAssistant');
            if (sendBtn) sendBtn.click();
          }
        }, 150);
      }
    });
  }
}

function openVerseOptions(ayahData, pageNum, cleanText) {
  state.quran.selectedVerse = {
    ...ayahData,
    pageNum,
    cleanText
  };

  const modal = document.getElementById('modalVerseOptions');
  const title = document.getElementById('verseModalTitle');
  const preview = document.getElementById('verseModalPreview');
  const tafsirBox = document.getElementById('tafsirContentArea');
  const transBox = document.getElementById('translationContentArea');

  if (title) title.textContent = `سورة ${ayahData.sura_name} - آية ${ayahData.aya}`;
  if (preview) preview.innerHTML = `﴿${cleanText}﴾ <span class="verse-symbol-glyph">۝${toArabicDigits(ayahData.aya)}</span>`;
  if (tafsirBox) tafsirBox.style.display = 'none';
  if (transBox) transBox.style.display = 'none';

  if (typeof updateReciterLabels === 'function') {
    updateReciterLabels();
  }

  if (modal) modal.classList.add('open');
}

// =========================================================================
// Quran Reciters Library (مكتبة كبار القراء المجانية المعتمدة)
// =========================================================================
const QURAN_RECITERS = [
  { id: 'Alafasy_128kbps', name: 'مشاري راشد العفاسي', riwaya: 'حفص عن عاصم', bit: '128 kbps' },
  { id: 'Abdul_Basit_Murattal_192kbps', name: 'عبد الباسط عبد الصمد (مرتل)', riwaya: 'حفص عن عاصم (مرتل)', bit: '192 kbps' },
  { id: 'Abdul_Basit_Mujawwad_128kbps', name: 'عبد الباسط عبد الصمد (مجود)', riwaya: 'حفص عن عاصم (مجود)', bit: '128 kbps' },
  { id: 'Minshawy_Murattal_128kbps', name: 'محمد صديق المنشاوي (مرتل)', riwaya: 'حفص عن عاصم (مرتل)', bit: '128 kbps' },
  { id: 'Minshawy_Mujawwad_192kbps', name: 'محمد صديق المنشاوي (مجود)', riwaya: 'حفص عن عاصم (مجود)', bit: '192 kbps' },
  { id: 'Husary_128kbps', name: 'محمود خليل الحصري (مرتل)', riwaya: 'حفص عن عاصم (مرتل)', bit: '128 kbps' },
  { id: 'Husary_Muallim_128kbps', name: 'محمود خليل الحصري (المعلم)', riwaya: 'المصحف المعلم', bit: '128 kbps' },
  { id: 'MaherAlMuaiqly128kbps', name: 'ماهر المعيقلي', riwaya: 'حفص عن عاصم', bit: '128 kbps' },
  { id: 'Abdurrahmaan_As-Sudais_192kbps', name: 'عبد الرحمن السديس', riwaya: 'حفص عن عاصم', bit: '192 kbps' },
  { id: 'Saood_ash-Shuraym_128kbps', name: 'سعود الشريم', riwaya: 'حفص عن عاصم', bit: '128 kbps' },
  { id: 'Ghamadi_40kbps', name: 'سعد الغامدي', riwaya: 'حفص عن عاصم', bit: '40 kbps' },
  { id: 'Yasser_Ad-Dussary_128kbps', name: 'ياسر الدوسري', riwaya: 'حفص عن عاصم', bit: '128 kbps' },
  { id: 'Abu_Bakr_Ash-Shaatree_128kbps', name: 'أبو بكر الشاطري', riwaya: 'حفص عن عاصم', bit: '128 kbps' },
  { id: 'Nasser_Alqatami_128kbps', name: 'ناصر القطامي', riwaya: 'حفص عن عاصم', bit: '128 kbps' },
  { id: 'Ali_Jaber_64kbps', name: 'علي جابر', riwaya: 'حفص عن عاصم', bit: '64 kbps' }
];

function getReciterObject(reciterId) {
  const id = reciterId || state.quran.reciterId || 'Alafasy_128kbps';
  return QURAN_RECITERS.find(r => r.id === id) || QURAN_RECITERS[0];
}

function getAyahAudioUrl(suraId, ayaNum, reciterId = null) {
  const r = getReciterObject(reciterId);
  const padSurah = String(suraId).padStart(3, '0');
  const padAyah = String(ayaNum).padStart(3, '0');
  return `https://everyayah.com/data/${r.id}/${padSurah}${padAyah}.mp3`;
}

function updateReciterLabels() {
  const r = getReciterObject(state.quran.reciterId);
  const audioBarReciter = document.getElementById('audioBarReciterText');
  if (audioBarReciter) {
    audioBarReciter.textContent = `القارئ: ${r.name} ▾`;
  }
  const settingsName = document.getElementById('settingsCurrentReciterName');
  const settingsSub = document.getElementById('settingsCurrentReciterSub');
  if (settingsName) settingsName.textContent = r.name;
  if (settingsSub) settingsSub.textContent = `${r.riwaya} (${r.bit})`;

  const verseReciterInfo = document.getElementById('verseModalReciterInfo');
  if (verseReciterInfo) verseReciterInfo.textContent = `القارئ: ${r.name}`;

  const labelPlay = document.getElementById('labelOptPlayAudio');
  if (labelPlay) labelPlay.textContent = `الاستماع لتلاوة الآية (بصوت ${r.name})`;
}

function setQuranReciter(reciterId) {
  const r = getReciterObject(reciterId);
  state.quran.reciterId = r.id;
  try {
    localStorage.setItem('sb_quran_reciter_id', r.id);
  } catch (_) {}

  updateReciterLabels();

  // If currently playing, seamlessly continue with new reciter
  if (state.quran.playingAudio && state.quran.currentPlaying) {
    const { sura, aya } = state.quran.currentPlaying;
    playAyahRecitation(sura, aya);
  }
}

let previewAudioReciter = null;

function setupReciterSelectionDialog() {
  const modal = document.getElementById('modalChooseReciter');
  const closeBtn = document.getElementById('btnCloseReciterModal');
  const searchInput = document.getElementById('inputSearchReciter');
  const listContainer = document.getElementById('recitersListGrid');

  const btnOpenTop = document.getElementById('btn-open-reciters');
  const btnOpenAudioBar = document.getElementById('audioBarReciterText');
  const btnChangeAudioBar = document.getElementById('btnChangeReciterFromBar');
  const btnOpenSettings = document.getElementById('btnChangeReciterFromSettings');
  const btnOpenVerse = document.getElementById('btnChangeReciterFromVerseModal');

  if (!modal || !listContainer) return;

  function closeModal() {
    modal.classList.remove('open');
    if (previewAudioReciter) {
      try { previewAudioReciter.pause(); } catch (_) {}
      previewAudioReciter = null;
    }
  }

  function openModal() {
    renderRecitersList(searchInput ? searchInput.value : '');
    modal.classList.add('open');
    if (searchInput) {
      setTimeout(() => searchInput.focus(), 150);
    }
  }

  window.openReciterSelectionDialog = openModal;

  if (closeBtn) closeBtn.addEventListener('click', closeModal);
  modal.addEventListener('click', (e) => {
    if (e.target === modal) closeModal();
  });

  if (btnOpenTop) btnOpenTop.addEventListener('click', openModal);
  if (btnOpenAudioBar) btnOpenAudioBar.addEventListener('click', openModal);
  if (btnChangeAudioBar) btnChangeAudioBar.addEventListener('click', openModal);
  if (btnOpenSettings) btnOpenSettings.addEventListener('click', openModal);
  if (btnOpenVerse) {
    btnOpenVerse.addEventListener('click', () => {
      const vModal = document.getElementById('modalVerseOptions');
      if (vModal) vModal.classList.remove('open');
      openModal();
    });
  }

  if (searchInput) {
    searchInput.addEventListener('input', () => {
      renderRecitersList(searchInput.value);
    });
  }

  function renderRecitersList(filterQuery = '') {
    listContainer.innerHTML = '';
    const q = (filterQuery || '').trim().toLowerCase();

    const filtered = QURAN_RECITERS.filter(r => {
      if (!q) return true;
      return r.name.toLowerCase().includes(q) || r.riwaya.toLowerCase().includes(q);
    });

    if (filtered.length === 0) {
      listContainer.innerHTML = '<div style="text-align: center; color: #64748B; padding: 24px;">لا يوجد قارئ مطابق للبحث</div>';
      return;
    }

    filtered.forEach(r => {
      const isSelected = r.id === state.quran.reciterId;
      const item = document.createElement('div');
      item.className = `reciter-card-item ${isSelected ? 'active' : ''}`;
      item.innerHTML = `
        <div class="reciter-card-info">
          <div class="reciter-name-row">
            <span class="reciter-name">${r.name}</span>
            <span class="reciter-badge-active">المحدد حالياً</span>
          </div>
          <span class="reciter-sub">${r.riwaya} • ${r.bit}</span>
        </div>
        <div class="reciter-actions">
          <button class="btn-reciter-preview" title="استماع تجريبي للبسملة">
            <span>▶</span>
            <span>استماع</span>
          </button>
          <button class="btn-reciter-select">
            ${isSelected ? 'محدد ✓' : 'اختيار'}
          </button>
        </div>
      `;

      // Preview audio button
      const btnPreview = item.querySelector('.btn-reciter-preview');
      btnPreview.addEventListener('click', (e) => {
        e.stopPropagation();
        if (previewAudioReciter) {
          try { previewAudioReciter.pause(); } catch (_) {}
        }
        btnPreview.textContent = 'تشغيل...';
        const url = getAyahAudioUrl(1, 1, r.id);
        previewAudioReciter = new Audio(url);
        previewAudioReciter.play().catch(() => {
          btnPreview.innerHTML = '<span>▶</span><span>استماع</span>';
        });
        previewAudioReciter.onended = () => {
          btnPreview.innerHTML = '<span>▶</span><span>استماع</span>';
        };
      });

      // Selection click
      item.addEventListener('click', () => {
        setQuranReciter(r.id);
        renderRecitersList(searchInput ? searchInput.value : '');
        setTimeout(closeModal, 180);
      });

      listContainer.appendChild(item);
    });
  }

  // Initial label update
  updateReciterLabels();
}

// =========================================================================
// Audio Recitation Player with Continuous Auto-Advance & Floating Bar
// =========================================================================
function playAyahRecitation(suraId, ayaNum) {
  if (state.quran.playingAudio) {
    try {
      state.quran.playingAudio.pause();
    } catch (_) {}
    state.quran.playingAudio = null;
  }

  // Find ayah in QURAN_PAGES to determine target page and surah name
  let targetPage = state.quran.currentPageNum;
  let ayahData = null;
  for (let p in window.QURAN_PAGES) {
    const found = window.QURAN_PAGES[p].find(x => x.sura === suraId && x.aya === ayaNum);
    if (found) {
      targetPage = parseInt(p, 10);
      ayahData = found;
      break;
    }
  }

  state.quran.currentPlaying = { sura: suraId, aya: ayaNum, page: targetPage };

  // Ensure target page is populated
  const pageBlock = document.getElementById(`page-block-${targetPage}`);
  if (pageBlock && pageBlock.dataset.rendered !== 'true') {
    renderPageBlockContent(pageBlock, targetPage);
  }

  // Highlight verse
  document.querySelectorAll('.verse-unit.highlighted').forEach(el => el.classList.remove('highlighted'));
  const verseEl = document.getElementById(`verse-${suraId}-${ayaNum}`);
  if (verseEl) {
    verseEl.classList.add('highlighted');
    const scrollArea = document.getElementById('quranScrollArea');
    if (scrollArea) {
      const vRect = verseEl.getBoundingClientRect();
      const aRect = scrollArea.getBoundingClientRect();
      const targetTop = scrollArea.scrollTop + (vRect.top - aRect.top) - (scrollArea.clientHeight / 2) + (vRect.height / 2);
      scrollArea.scrollTo({ top: Math.max(0, targetTop), behavior: 'smooth' });
    }
  }

  // Update Floating Audio Player Bar
  updateFloatingAudioBarUI(suraId, ayaNum, ayahData?.sura_name, true);

  const audioUrl = getAyahAudioUrl(suraId, ayaNum);

  const audio = new Audio(audioUrl);
  state.quran.playingAudio = audio;

  audio.play().catch(e => {
    console.warn('Audio playback error:', e);
    if (verseEl) verseEl.classList.remove('highlighted');
    updateFloatingAudioBarUI(suraId, ayaNum, ayahData?.sura_name, false);
  });

  audio.onended = () => {
    // 1. Check Smart Hifz repetition
    if (state.quran.hifzRepeatCount > 1) {
      if (state.quran.hifzCurrentIteration < state.quran.hifzRepeatCount) {
        state.quran.hifzCurrentIteration++;
        updateHifzRepeatBadge();
        playAyahRecitation(suraId, ayaNum);
        return;
      } else {
        state.quran.hifzCurrentIteration = 1;
        updateHifzRepeatBadge();
      }
    }

    // 2. Check Continuous Recitation (Auto-Advance)
    if (state.quran.continuousAudio) {
      advanceToNextAyah(suraId, ayaNum);
    } else {
      if (verseEl) verseEl.classList.remove('highlighted');
      if (audioBottomBtn) audioBottomBtn.classList.remove('active');
      state.quran.playingAudio = null;
      updateFloatingAudioBarUI(suraId, ayaNum, ayahData?.sura_name, false);
    }
  };
}

function advanceToNextAyah(suraId, ayaNum) {
  const surahMeta = window.QURAN_SURAHS.find(s => s.id === suraId);
  const maxAyas = surahMeta ? surahMeta.ayah_count : 7;

  let nextSura = suraId;
  let nextAya = ayaNum + 1;

  if (nextAya > maxAyas) {
    if (suraId < 114) {
      nextSura = suraId + 1;
      nextAya = 1;
    } else {
      // Reached the end of Quran (Surah 114)
      stopAudioRecitation();
      return;
    }
  }

  state.quran.hifzCurrentIteration = 1;
  updateHifzRepeatBadge();
  playAyahRecitation(nextSura, nextAya);
}

function advanceToPrevAyah() {
  if (!state.quran.currentPlaying) return;
  const { sura, aya } = state.quran.currentPlaying;

  let prevSura = sura;
  let prevAya = aya - 1;

  if (prevAya < 1) {
    if (sura > 1) {
      prevSura = sura - 1;
      const prevSurahMeta = window.QURAN_SURAHS.find(s => s.id === prevSura);
      prevAya = prevSurahMeta ? prevSurahMeta.ayah_count : 1;
    } else {
      return; // Already at Surah 1, Aya 1
    }
  }

  state.quran.hifzCurrentIteration = 1;
  updateHifzRepeatBadge();
  playAyahRecitation(prevSura, prevAya);
}

function stopAudioRecitation() {
  if (state.quran.playingAudio) {
    try {
      state.quran.playingAudio.pause();
    } catch (_) {}
    state.quran.playingAudio = null;
  }
  document.querySelectorAll('.verse-unit.highlighted').forEach(el => el.classList.remove('highlighted'));
  const audioBottomBtn = document.getElementById('bb-btn-audio');
  if (audioBottomBtn) audioBottomBtn.classList.remove('active');

  const bar = document.getElementById('floatingAudioBar');
  if (bar) bar.style.display = 'none';
}

function updateFloatingAudioBarUI(suraId, ayaNum, suraName, isPlaying) {
  const bar = document.getElementById('floatingAudioBar');
  if (!bar) return;

  bar.style.display = 'flex';

  const verseLabel = document.getElementById('audioBarVerseText');
  if (verseLabel) {
    const sName = suraName || window.QURAN_SURAHS.find(s => s.id === suraId)?.name || '';
    verseLabel.textContent = `سورة ${sName} - آية ${ayaNum}`;
  }

  const reciterLabel = document.getElementById('audioBarReciterText');
  if (reciterLabel) {
    const r = getReciterObject(state.quran.reciterId);
    reciterLabel.textContent = `القارئ: ${r.name} ▾`;
  }

  const svgIcon = document.getElementById('svgAudioPlayPause');
  if (svgIcon) {
    if (isPlaying) {
      svgIcon.innerHTML = '<path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>';
    } else {
      svgIcon.innerHTML = '<path d="M8 5v14l11-7z"/>';
    }
  }

  const chk = document.getElementById('chkContinuousAudio');
  if (chk) {
    chk.checked = state.quran.continuousAudio;
  }

  updateHifzRepeatBadge();
}

function updateHifzRepeatBadge() {
  const badge = document.getElementById('audioRepeatBadge');
  if (!badge) return;
  if (state.quran.hifzRepeatCount > 1) {
    badge.style.display = 'inline-block';
    const total = state.quran.hifzRepeatCount >= 999 ? '∞' : state.quran.hifzRepeatCount;
    badge.textContent = `تكرار: ${state.quran.hifzCurrentIteration}/${total}`;
  } else {
    badge.style.display = 'none';
  }
}

function setupFloatingAudioBar() {
  const btnPrev = document.getElementById('btnAudioPrev');
  const btnPlayPause = document.getElementById('btnAudioPlayPause');
  const btnNext = document.getElementById('btnAudioNext');
  const btnStop = document.getElementById('btnAudioStop');
  const btnClose = document.getElementById('btnAudioCloseBar');
  const chkContinuous = document.getElementById('chkContinuousAudio');

  if (btnPrev) btnPrev.addEventListener('click', advanceToPrevAyah);
  if (btnNext) {
    btnNext.addEventListener('click', () => {
      if (state.quran.currentPlaying) {
        advanceToNextAyah(state.quran.currentPlaying.sura, state.quran.currentPlaying.aya);
      }
    });
  }
  if (btnPlayPause) {
    btnPlayPause.addEventListener('click', () => {
      if (!state.quran.playingAudio) {
        if (state.quran.currentPlaying) {
          playAyahRecitation(state.quran.currentPlaying.sura, state.quran.currentPlaying.aya);
        } else {
          const curPage = state.quran.currentPageNum || 1;
          const pageAyahs = window.QURAN_PAGES[String(curPage)] || [];
          const firstA = pageAyahs.find(x => !x.isBasmala || x.aya > 0);
          if (firstA) playAyahRecitation(firstA.sura, firstA.aya);
        }
      } else {
        if (state.quran.playingAudio.paused) {
          state.quran.playingAudio.play();
          updateFloatingAudioBarUI(state.quran.currentPlaying?.sura, state.quran.currentPlaying?.aya, null, true);
        } else {
          state.quran.playingAudio.pause();
          updateFloatingAudioBarUI(state.quran.currentPlaying?.sura, state.quran.currentPlaying?.aya, null, false);
        }
      }
    });
  }
  if (btnStop) btnStop.addEventListener('click', stopAudioRecitation);
  if (btnClose) btnClose.addEventListener('click', stopAudioRecitation);

  if (chkContinuous) {
    chkContinuous.addEventListener('change', (e) => {
      state.quran.continuousAudio = e.target.checked;
      localStorage.setItem('sb_quran_continuous_audio', state.quran.continuousAudio);
    });
  }
}

// Font Settings & Appearance Dialog with Translation Controls
function setupFontSettingsDialog() {
  const modal = document.getElementById('modalFontSettings');
  const openBtn = document.getElementById('btn-open-font-settings');
  const closeBtn = document.getElementById('btnCloseFontModal');
  const slider = document.getElementById('sliderFontSize');

  const btnAmiri = document.getElementById('btnFontAmiri');
  const btnUthman = document.getElementById('btnFontUthman');
  const btnKitab = document.getElementById('btnFontKitab');

  const btnTransNone = document.getElementById('btnTransNone');
  const btnTransEn = document.getElementById('btnTransEn');
  const btnTransId = document.getElementById('btnTransId');

  const btnParchment = document.getElementById('btnThemeParchment');
  const btnWhite = document.getElementById('btnThemeWhite');
  const btnDark = document.getElementById('btnThemeDark');

  if (!modal) return;

  function closeModal() {
    modal.classList.remove('open');
  }

  if (openBtn) openBtn.addEventListener('click', () => modal.classList.add('open'));
  if (closeBtn) closeBtn.addEventListener('click', closeModal);
  modal.addEventListener('click', (e) => {
    if (e.target === modal) closeModal();
  });

  // Font Size Slider
  if (slider) {
    slider.addEventListener('input', (e) => {
      const size = e.target.value;
      state.quran.fontSize = size;
      document.querySelectorAll('.page-verses-text').forEach(el => {
        el.style.fontSize = `${size}px`;
      });
    });
  }

  // Font Family Toggles
  function setFontFamily(fontName, activeBtn) {
    state.quran.fontFamily = fontName;
    [btnAmiri, btnUthman, btnKitab].forEach(b => b?.classList.remove('active'));
    activeBtn?.classList.add('active');
    document.documentElement.style.setProperty('--font-quran', `'${fontName}', serif`);
  }

  if (btnAmiri) btnAmiri.addEventListener('click', () => setFontFamily('AmiriQuran', btnAmiri));
  if (btnUthman) btnUthman.addEventListener('click', () => setFontFamily('UthmanTaha', btnUthman));
  if (btnKitab) btnKitab.addEventListener('click', () => setFontFamily('Kitab', btnKitab));

  // Translation Toggles
  function setTranslationLang(lang, activeBtn) {
    state.quran.translationLang = lang;
    localStorage.setItem('sb_quran_trans_lang', lang);
    [btnTransNone, btnTransEn, btnTransId].forEach(b => b?.classList.remove('active'));
    activeBtn?.classList.add('active');
    refreshQuranTranslations();
  }

  // Init translation active state
  if (state.quran.translationLang === 'en') {
    [btnTransNone, btnTransEn, btnTransId].forEach(b => b?.classList.remove('active'));
    btnTransEn?.classList.add('active');
  } else if (state.quran.translationLang === 'id') {
    [btnTransNone, btnTransEn, btnTransId].forEach(b => b?.classList.remove('active'));
    btnTransId?.classList.add('active');
  }

  if (btnTransNone) btnTransNone.addEventListener('click', () => setTranslationLang('none', btnTransNone));
  if (btnTransEn) btnTransEn.addEventListener('click', () => setTranslationLang('en', btnTransEn));
  if (btnTransId) btnTransId.addEventListener('click', () => setTranslationLang('id', btnTransId));

  // Authentic 8 Android Themes
  const themeIds = ['Creamy', 'Night', 'Lunar', 'Burgundy', 'Emerald', 'Sky', 'Pink', 'Crimson'];
  themeIds.forEach(name => {
    const key = name.toLowerCase();
    const btn = document.getElementById(`btnTheme${name}`);
    if (btn) {
      btn.addEventListener('click', () => {
        applyTheme(key);
      });
    }
  });

  // Highlight current active theme in settings dialog
  const curTheme = localStorage.getItem('sb_app_theme') || 'creamy';
  document.querySelectorAll('.theme-choice-btn').forEach(b => {
    b.classList.toggle('active', b.getAttribute('data-theme') === curTheme);
  });
}

// Refresh Inline Translations Across Rendered Pages
function refreshQuranTranslations() {
  document.querySelectorAll('.verse-trans-line').forEach(el => el.remove());
  if (state.quran.translationLang === 'none') return;

  const renderedBlocks = document.querySelectorAll('.page-mushaf-block[data-rendered="true"]');
  renderedBlocks.forEach(pageEl => {
    const pageNum = pageEl.getAttribute('data-page');
    const pageAyahs = window.QURAN_PAGES[pageNum] || [];
    pageAyahs.forEach(a => {
      if (a.isBasmala && a.aya === 0) return;
      const transText = state.quran.translationLang === 'en' ? a.translation_en : a.translation_id;
      if (!transText) return;

      const verseEl = document.getElementById(`verse-${a.sura}-${a.aya}`);
      if (verseEl) {
        const transDiv = document.createElement('div');
        transDiv.className = 'verse-trans-line';
        transDiv.setAttribute('data-trans-aya', `${a.sura}-${a.aya}`);
        transDiv.textContent = `[${a.sura}:${a.aya}] ${transText}`;
        verseEl.parentNode.insertBefore(transDiv, verseEl.nextSibling);
      }
    });
  });
}

// =========================================================================
// QuranTextMatcher: محرك مطابقة وتحليل نصوص التلاوة الصوتية المتسامح
// مستوحى ومطابق 100% لخوارزمية التطبيق الأصلية في أندرويد (QuranTextMatcher.kt)
// =========================================================================
const QuranTextMatcher = {
  cleanIntraWordSpacers(text) {
    if (!text) return '';
    return text
      .replace(/\u202F/g, '') // Narrow No-Break Space (NNBSP)
      .replace(/\u200B/g, '') // Zero Width Space
      .replace(/\u200C/g, '') // Zero Width Non-Joiner
      .replace(/\u200D/g, '') // Zero Width Joiner
      .replace(/\uFEFF/g, '') // BOM
      .replace(/\u00A0/g, ' ');
  },

  normalize(text) {
    if (!text) return '';
    return this.cleanIntraWordSpacers(text)
      .replace(/[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED]/g, '') // Harakat, Dagger Alif, Tajweed signs
      .replace(/ـ/g, '') // Tatweel
      .replace(/[إأآٱءا]/g, 'ا')
      .replace(/[ىیئۍێے]/g, 'ي')
      .replace(/[ةھہ]/g, 'ه')
      .replace(/[ؤۥۄ]/g, 'و')
      .replace(/[کگ]/g, 'ك')
      .replace(/ں/g, 'ن')
      .replace(/ڤ/g, 'ف')
      .replace(/پ/g, 'ب')
      .replace(/[\u06DD۝\d\u0660-\u0669\u06F0-\u06F9]/g, '') // End-of-ayah signs and digits
      .replace(/[^\u0621-\u064A\s]/g, '') // Keep Arabic letters only
      .replace(/\s+/g, ' ')
      .trim();
  },

  extractWords(rawVerseText, sura = -1, aya = -1) {
    let clean = this.cleanIntraWordSpacers(rawVerseText || '');
    if (sura !== 1 && aya === 1) {
      clean = clean.replace(/^(بِسۡ?مِ\s+ٱللَّهِ\s+ٱلرَّحۡ?مَـٰنِ\s+ٱلرَّحِیمِ|بِسْمِ\s+اللَّهِ\s+الرَّحْمَٰنِ\s+الرَّحِيمِ)\s*/, '');
    }
    return clean
      .split(/\s+/)
      .map(w => w.replace(/[\u06DD۝\d\u0660-\u0669\u06F0-\u06F9]/g, '').trim())
      .filter(w => w.length > 0 && this.normalize(w).length > 0);
  },

  levenshteinDistance(s1, s2) {
    const l1 = s1.length;
    const l2 = s2.length;
    const dp = Array.from({ length: l1 + 1 }, () => Array(l2 + 1).fill(0));

    for (let i = 0; i <= l1; i++) dp[i][0] = i;
    for (let j = 0; j <= l2; j++) dp[0][j] = j;

    for (let i = 1; i <= l1; i++) {
      for (let j = 1; j <= l2; j++) {
        const cost = s1[i - 1] === s2[j - 1] ? 0 : 1;
        dp[i][j] = Math.min(
          dp[i - 1][j] + 1,
          dp[i][j - 1] + 1,
          dp[i - 1][j - 1] + cost
        );
      }
    }
    return dp[l1][l2];
  },

  isWordMatchTolerant(spoken, target) {
    const s = this.normalize(spoken);
    const t = this.normalize(target);
    if (!s || !t) return false;
    if (s === t) return true;

    // 1. Prefix "ال"
    const sNoAl = s.startsWith('ال') && s.length > 3 ? s.substring(2) : s;
    const tNoAl = t.startsWith('ال') && t.length > 3 ? t.substring(2) : t;
    if (sNoAl === tNoAl || sNoAl === t || s === tNoAl) return true;

    // 2. Prefixes (و، ف، ب، ل، ك)
    const stripPrefix = w => w.replace(/^[وفبلك]/, '');
    const sNoPre = stripPrefix(sNoAl);
    const tNoPre = stripPrefix(tNoAl);
    if (sNoPre && (sNoPre === tNoPre || sNoPre === tNoAl || sNoPre === t)) return true;

    // 3. Phonetic shifts (ص <-> س، ذ <-> ز، ض <-> ظ، ط <-> ت)
    const toPhonetic = w => w.replace(/ص/g, 'س').replace(/ذ/g, 'ز').replace(/ظ/g, 'ض').replace(/ط/g, 'ت');
    const sPho = toPhonetic(sNoAl);
    const tPho = toPhonetic(tNoAl);
    if (sPho === tPho) return true;

    // 4. Levenshtein fuzzy distance
    const dist = this.levenshteinDistance(sPho, tPho);
    const maxLen = Math.max(sPho.length, tPho.length);
    if (maxLen <= 3 && dist <= 1) return true;
    if (maxLen >= 4 && maxLen <= 6 && dist <= 2) return true;
    if (maxLen > 6 && dist <= 3) return true;

    const sim = 1.0 - (dist / maxLen);
    return sim >= 0.55;
  },

  matchWordsTolerant(verseWords, spokenText, previousMatchedCount = 0) {
    if (!verseWords || verseWords.length === 0) return 0;
    const spokenWords = this.normalize(spokenText).split(' ').filter(w => w.length > 0);
    if (spokenWords.length === 0) return previousMatchedCount;

    let targetIndex = previousMatchedCount;
    let spokenIndex = 0;

    while (targetIndex < verseWords.size && spokenIndex < spokenWords.length) {
      const targetWord = verseWords[targetIndex];
      const spokenWord = spokenWords[spokenIndex];

      if (this.isWordMatchTolerant(spokenWord, targetWord)) {
        targetIndex++;
        spokenIndex++;
      } else {
        let foundAhead = false;
        for (let look = 1; look <= 3; look++) {
          if (spokenIndex + look < spokenWords.length) {
            if (this.isWordMatchTolerant(spokenWords[spokenIndex + look], targetWord)) {
              targetIndex++;
              spokenIndex += (look + 1);
              foundAhead = true;
              break;
            }
          }
        }
        if (!foundAhead) {
          spokenIndex++;
        }
      }
    }
    return Math.max(previousMatchedCount, Math.min(targetIndex, verseWords.length));
  }
};

// =========================================================================
// Voice-Driven Recitation Tracking (وضع التسميع الذكي المتطور والمحسن)
// يشمل تغبيش كل الآيات وإظهارها عند التلاوة، معايرة الضوضاء التلقائية، وQuranTextMatcher
// =========================================================================
function setupVoiceSpeechTracker() {
  const btnToggle = document.getElementById('btn-toggle-speech-tracker');
  const bar = document.getElementById('voiceSpeechTrackerBar');
  const pulse = document.getElementById('voicePulseIndicator');
  const meterFill = document.getElementById('voiceMeterFill');
  const thresholdMarker = document.getElementById('voiceThresholdMarker');
  const transcriptEl = document.getElementById('voiceLiveTranscript');
  const targetLabel = document.getElementById('voiceTargetVerseLabel');
  const recitedCountBadge = document.getElementById('voiceRecitedCountBadge');
  const btnSensitivity = document.getElementById('btnVoiceSensitivity');
  const btnToggleMask = document.getElementById('btnToggleRecitationMask');
  const labelToggleMask = document.getElementById('labelToggleMask');
  const btnPeekVerse = document.getElementById('btnVoicePeekVerse');
  const labelPeekVerse = document.getElementById('labelPeekVerse');
  const btnListen = document.getElementById('btnVoiceListenCurrent');
  const labelListen = document.getElementById('labelListenVerse');
  const btnAdvance = document.getElementById('btnVoiceAdvanceNext');
  const closeBtn = document.getElementById('btnCloseVoiceTracker');
  const scrollArea = document.getElementById('quranScrollArea');

  let audioContext = null;
  let analyser = null;
  let micStream = null;
  let vadAnimId = null;
  let recognition = null;
  let previewAudio = null;

  // Tracking state
  let currentTargetSura = 1;
  let currentTargetAya = 1;
  let isSpeaking = false;
  let speechStartTime = 0;
  let lastSoundTime = 0;
  let hasSpokenCurrentVerse = false;
  let recitedCount = 0;
  let isPeeked = false;

  // Sensitivity presets: ultra (margin +1: triggers on faint whisper/low voice), normal (margin +3), strict (margin +7)
  let sensitivityMode = localStorage.getItem('sb_voice_sensitivity') || 'ultra';
  let sensitivityMargin = sensitivityMode === 'strict' ? 7 : (sensitivityMode === 'normal' ? 3 : 1);
  let noiseFloor = 3; // Auto-calibrating ambient baseline (starts low for immediate responsiveness)

  // All-verses masking state (default to true: blur all verses in recitation mode)
  state.quran.recitationMasking = (localStorage.getItem('sb_recitation_masking') !== 'false');

  function updateSensitivityUI() {
    if (!btnSensitivity) return;
    if (sensitivityMode === 'ultra') {
      btnSensitivity.textContent = '⚡ الحساسية: فائق التساهل (همس)';
      sensitivityMargin = 1;
    } else if (sensitivityMode === 'normal') {
      btnSensitivity.textContent = '⚡ الحساسية: متوازن';
      sensitivityMargin = 3;
    } else {
      btnSensitivity.textContent = '⚡ الحساسية: دقيق';
      sensitivityMargin = 7;
    }
  }

  function cycleSensitivity() {
    if (sensitivityMode === 'ultra') sensitivityMode = 'normal';
    else if (sensitivityMode === 'normal') sensitivityMode = 'strict';
    else sensitivityMode = 'ultra';

    localStorage.setItem('sb_voice_sensitivity', sensitivityMode);
    updateSensitivityUI();
  }

  if (btnSensitivity) {
    updateSensitivityUI();
    btnSensitivity.addEventListener('click', cycleSensitivity);
  }

  function updateMaskToggleButtonUI() {
    if (!btnToggleMask || !labelToggleMask) return;
    if (state.quran.recitationMasking) {
      btnToggleMask.classList.add('active');
      labelToggleMask.textContent = 'تغبيش كل الآيات';
      if (scrollArea) scrollArea.classList.add('recitation-mode-active');
    } else {
      btnToggleMask.classList.remove('active');
      labelToggleMask.textContent = 'إظهار كل الآيات';
      if (scrollArea) scrollArea.classList.remove('recitation-mode-active');
    }
  }

  function playSuccessChime() {
    // Sound cancelled completely upon user request (إلغاء الصوت نهائياً عند إظهار الآية أو الانتقال)
  }

  function getNextVerseTarget(sura, aya) {
    const sMeta = window.QURAN_SURAHS.find(s => s.id === sura);
    const maxA = sMeta ? sMeta.ayah_count : 7;
    if (aya < maxA) return { sura, aya: aya + 1 };
    if (sura < 114) return { sura: sura + 1, aya: 1 };
    return { sura: 1, aya: 1 };
  }

  function stopAudioPreview() {
    if (previewAudio) {
      try {
        previewAudio.pause();
      } catch (_) {}
      previewAudio = null;
    }
    if (btnListen && labelListen) {
      btnListen.classList.remove('active');
      labelListen.textContent = 'استماع';
    }
  }

  function applyTargetVerseState(sura, aya, options = {}) {
    isPeeked = false;
    if (labelPeekVerse) labelPeekVerse.textContent = 'كشف الآية';
    if (btnPeekVerse) btnPeekVerse.classList.remove('active');

    // Remove previous target markers
    document.querySelectorAll('.verse-unit.recitation-current-target').forEach(el => {
      el.classList.remove('recitation-current-target', 'revealing-with-speech', 'peek-revealed');
    });

    const verseEl = document.getElementById(`verse-${sura}-${aya}`);
    if (verseEl) {
      verseEl.classList.remove('revealing-with-speech', 'peek-revealed');
      verseEl.classList.add('recitation-current-target');
      const scrollArea = document.getElementById('quranScrollArea');
      if (scrollArea) {
        const vRect = verseEl.getBoundingClientRect();
        const aRect = scrollArea.getBoundingClientRect();
        const targetTop = scrollArea.scrollTop + (vRect.top - aRect.top) - (scrollArea.clientHeight / 2) + (vRect.height / 2);
        scrollArea.scrollTo({ top: Math.max(0, targetTop), behavior: 'smooth' });
      }
    }

    const sName = window.QURAN_SURAHS.find(s => s.id === sura)?.name || '';
    if (targetLabel) targetLabel.textContent = `سورة ${sName} - آية ${aya}`;

    if (transcriptEl) {
      if (options.reasonText) {
        transcriptEl.textContent = options.reasonText;
      } else if (state.quran.recitationMasking) {
        transcriptEl.textContent = 'اقرأ بصوتك.. الآيات مغوشة بالكامل وتظهر الآية تلقائياً أثناء تلاوتك أو انقر لكشفها';
      } else {
        transcriptEl.textContent = 'تحدث أو اقرأ بصوتك، وسينتقل تلقائياً مع توقف صوتك أو نطق أي كلمة...';
      }
    }

    hasSpokenCurrentVerse = false;
    isSpeaking = false;
  }

  function revealCurrentVerseWithSpeech() {
    const verseEl = document.getElementById(`verse-${currentTargetSura}-${currentTargetAya}`);
    if (verseEl) {
      verseEl.classList.add('revealing-with-speech');
      if (transcriptEl && (!transcriptEl.textContent || transcriptEl.textContent.includes('اقرأ بصوتك..'))) {
        transcriptEl.textContent = '✨ أحسنت! صوتك مسموع والآية ظاهرة بوضوح الآن...';
      }
    }
  }

  function togglePeekCurrentVerse() {
    const verseEl = document.getElementById(`verse-${currentTargetSura}-${currentTargetAya}`);
    if (!verseEl) return;

    isPeeked = !isPeeked;
    if (isPeeked) {
      verseEl.classList.add('peek-revealed');
      if (btnPeekVerse) btnPeekVerse.classList.add('active');
      if (labelPeekVerse) labelPeekVerse.textContent = 'إخفاء الآية';
      if (transcriptEl) transcriptEl.textContent = '👁 تم كشف الآية لمراجعتها.. اقرأها ثم انتقل للتالية';
    } else {
      verseEl.classList.remove('peek-revealed');
      if (btnPeekVerse) btnPeekVerse.classList.remove('active');
      if (labelPeekVerse) labelPeekVerse.textContent = 'كشف الآية';
      if (transcriptEl) transcriptEl.textContent = '🔒 تم إعادة التغبيش.. تابع تلاوتك بصوتك';
    }
  }

  function toggleAudioPreview() {
    if (previewAudio && !previewAudio.paused) {
      stopAudioPreview();
      return;
    }

    stopAudioPreview();

    const audioUrl = getAyahAudioUrl(currentTargetSura, currentTargetAya);

    previewAudio = new Audio(audioUrl);
    if (btnListen && labelListen) {
      btnListen.classList.add('active');
      labelListen.textContent = 'جاري الاستماع...';
    }

    previewAudio.onended = () => {
      stopAudioPreview();
    };

    previewAudio.onerror = () => {
      stopAudioPreview();
      if (transcriptEl) transcriptEl.textContent = 'تعذر تشغيل الصوت.. تحقق من اتصال الإنترنت';
    };

    previewAudio.play().catch(e => {
      console.warn('Preview audio play failed:', e);
      stopAudioPreview();
    });
  }

  function advanceRecitationToNext(reasonText = '') {
    stopAudioPreview();
    // Sound cancelled completely upon user request (لا يوجد رنين أو صوت عند الانتقال أو الإظهار)

    // Mark previous verse as completed (permanently unblurred and verified)
    const prevEl = document.getElementById(`verse-${currentTargetSura}-${currentTargetAya}`);
    if (prevEl) {
      prevEl.classList.remove('recitation-current-target', 'revealing-with-speech', 'peek-revealed');
      prevEl.classList.add('recitation-completed', 'voice-matched');
      setTimeout(() => prevEl.classList.remove('voice-matched'), 3500);
    }

    // Increment recited verses counter
    recitedCount++;
    if (recitedCountBadge) {
      recitedCountBadge.textContent = `🎯 سُمِّع: ${toArabicDigits(recitedCount)}`;
    }

    const next = getNextVerseTarget(currentTargetSura, currentTargetAya);
    currentTargetSura = next.sura;
    currentTargetAya = next.aya;

    // Find target page
    let targetPage = state.quran.currentPageNum;
    for (let p in window.QURAN_PAGES) {
      if (window.QURAN_PAGES[p].some(x => x.sura === currentTargetSura && x.aya === currentTargetAya)) {
        targetPage = parseInt(p, 10);
        break;
      }
    }

    jumpToQuranPage(targetPage, currentTargetAya);
    applyTargetVerseState(currentTargetSura, currentTargetAya, { reasonText: reasonText || '✨ أحسنت التلاوة! ننتقل للآية التالية...' });
  }

  // 1. Direct Web Audio API Microphone with Dynamic Noise-Floor Calibration
  async function startLocalAudioVAD() {
    try {
      micStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        },
        video: false
      });

      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (!AudioCtx) return;
      audioContext = new AudioCtx();
      if (audioContext.state === 'suspended') {
        await audioContext.resume();
      }

      analyser = audioContext.createAnalyser();
      analyser.fftSize = 256;
      analyser.smoothingTimeConstant = 0.3;
      const source = audioContext.createMediaStreamSource(micStream);
      source.connect(analyser);

      const bufferLength = analyser.frequencyBinCount;
      const dataArray = new Uint8Array(bufferLength);

      function vadLoop() {
        if (!state.quran.speechTrackerActive) return;
        analyser.getByteFrequencyData(dataArray);

        // Calculate average volume
        let sum = 0;
        for (let i = 0; i < bufferLength; i++) {
          sum += dataArray[i];
        }
        const avg = sum / bufferLength;

        // Auto-calibrate background noise floor dynamically
        if (avg < noiseFloor + 5) {
          noiseFloor = (noiseFloor * 0.95) + (avg * 0.05);
        }

        const dynamicThreshold = Math.max(1.8, Math.round(noiseFloor + sensitivityMargin));

        // Update visual threshold marker
        const thresholdPct = Math.min(95, Math.round((dynamicThreshold / 35) * 100));
        if (thresholdMarker) thresholdMarker.style.left = `${thresholdPct}%`;

        // Update live volume meter
        const normalizedVol = Math.min(100, Math.round((avg / 35) * 100));
        if (meterFill) meterFill.style.width = `${normalizedVol}%`;

        const now = Date.now();

        // High responsiveness: triggers instantly even on faint whisper, breathing, or low-quality mic voice
        const voiceDetected = (avg > dynamicThreshold) || (sensitivityMode === 'ultra' && avg >= 2.0);

        if (voiceDetected) {
          // Instant 0ms verse reveal
          revealCurrentVerseWithSpeech();

          if (!isSpeaking) {
            isSpeaking = true;
            speechStartTime = now;
          }
          lastSoundTime = now;
          hasSpokenCurrentVerse = true;
          if (pulse) pulse.classList.add('speaking');
        } else {
          if (pulse) pulse.classList.remove('speaking');

          // Snappy progression: Natural Waqf pause detection (shorter requirements for fast response)
          if (isSpeaking && hasSpokenCurrentVerse) {
            const speakingDuration = lastSoundTime - speechStartTime;
            const pauseDuration = now - lastSoundTime;

            const minSpeaking = sensitivityMode === 'ultra' ? 350 : (sensitivityMode === 'normal' ? 500 : 700);
            const minPause = sensitivityMode === 'ultra' ? 650 : (sensitivityMode === 'normal' ? 850 : 1000);

            if (speakingDuration >= minSpeaking && pauseDuration >= minPause) {
              advanceRecitationToNext('✨ أحسنت التلاوة! ننتقل للآية التالية تلقائياً...');
            }
          }
        }

        vadAnimId = requestAnimationFrame(vadLoop);
      }

      vadLoop();
    } catch (e) {
      console.warn('Microphone getUserMedia error:', e);
      if (transcriptEl) transcriptEl.textContent = 'يرجى التأكد من توصيل الميكروفون والسماح له بالعمل في النظام.';
    }
  }

  // 2. Tolerant Speech Recognition (ASR) with QuranTextMatcher
  function startSpeechASR() {
    const SpeechRec = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRec) return;

    try {
      recognition = new SpeechRec();
      recognition.lang = 'ar-SA';
      recognition.continuous = true;
      recognition.interimResults = true;

      recognition.onresult = (event) => {
        let text = '';
        for (let i = event.resultIndex; i < event.results.length; ++i) {
          text += event.results[i][0].transcript;
        }
        if (text && transcriptEl) {
          transcriptEl.textContent = text;
        }

        // Reveal verse immediately when words are heard
        revealCurrentVerseWithSpeech();

        // Get target verse words
        let rawVerseText = '';
        for (let p in window.QURAN_PAGES) {
          const found = window.QURAN_PAGES[p].find(x => x.sura === currentTargetSura && x.aya === currentTargetAya);
          if (found) {
            rawVerseText = found.text;
            break;
          }
        }

        if (rawVerseText && text) {
          const verseWords = QuranTextMatcher.extractWords(rawVerseText, currentTargetSura, currentTargetAya);
          const matchedWords = QuranTextMatcher.matchWordsTolerant(verseWords, text, 0);

          // Fast and lenient matching: if even 1 word matches in short verses or >= 20% match in longer verses:
          if (matchedWords >= 1 || (verseWords.length > 4 && matchedWords >= Math.ceil(verseWords.length * 0.2))) {
            advanceRecitationToNext(`✨ تم التحقق من تلاوتك الكريمة بنجاح!`);
          }
        }
      };

      recognition.onerror = (e) => {
        console.warn('SpeechRecognition ASR info:', e.error);
      };

      recognition.onend = () => {
        if (state.quran.speechTrackerActive && recognition) {
          try { recognition.start(); } catch (_) {}
        }
      };

      recognition.start();
    } catch (e) {
      console.warn('ASR start skipped:', e);
    }
  }

  function startTracker() {
    state.quran.speechTrackerActive = true;
    recitedCount = 0;
    if (recitedCountBadge) {
      recitedCountBadge.textContent = `🎯 سُمِّع: ٠`;
    }

    updateMaskToggleButtonUI();
    updateSensitivityUI();

    if (bar) bar.style.display = 'flex';
    if (btnToggle) {
      btnToggle.style.background = '#EF4444';
      btnToggle.style.color = '#FFFFFF';
    }

    // Determine current visible verse as starting target
    const curP = state.quran.currentPageNum || 1;
    const pageAyahs = window.QURAN_PAGES[String(curP)] || [];
    const firstA = pageAyahs.find(x => !x.isBasmala || x.aya > 0) || { sura: 1, aya: 1 };
    currentTargetSura = firstA.sura;
    currentTargetAya = firstA.aya;

    applyTargetVerseState(currentTargetSura, currentTargetAya);

    startLocalAudioVAD();
    startSpeechASR();
  }

  function stopTracker() {
    state.quran.speechTrackerActive = false;
    stopAudioPreview();

    if (vadAnimId) {
      cancelAnimationFrame(vadAnimId);
      vadAnimId = null;
    }
    if (micStream) {
      micStream.getTracks().forEach(t => t.stop());
      micStream = null;
    }
    if (audioContext) {
      try { audioContext.close(); } catch (_) {}
      audioContext = null;
    }
    if (recognition) {
      try { recognition.stop(); } catch (_) {}
      recognition = null;
    }
    if (bar) bar.style.display = 'none';
    if (btnToggle) {
      btnToggle.style.background = 'transparent';
      btnToggle.style.color = 'var(--text-dark)';
    }

    // Remove all-verses blur mode from scroll area
    if (scrollArea) scrollArea.classList.remove('recitation-mode-active');

    // Clean up all recitation classes
    document.querySelectorAll('.verse-unit').forEach(el => {
      el.classList.remove('voice-matched', 'recitation-current-target', 'revealing-with-speech', 'peek-revealed', 'recitation-completed');
    });
  }

  // Toggle Masking Button (تغبيش كل الآيات)
  if (btnToggleMask) {
    btnToggleMask.addEventListener('click', () => {
      state.quran.recitationMasking = !state.quran.recitationMasking;
      localStorage.setItem('sb_recitation_masking', state.quran.recitationMasking ? 'true' : 'false');
      updateMaskToggleButtonUI();
      applyTargetVerseState(currentTargetSura, currentTargetAya);
    });
  }

  // Peek Verse Button
  if (btnPeekVerse) {
    btnPeekVerse.addEventListener('click', togglePeekCurrentVerse);
  }

  // Listen to Verse Button
  if (btnListen) {
    btnListen.addEventListener('click', toggleAudioPreview);
  }

  // Toggle Speech Tracker
  if (btnToggle) {
    btnToggle.addEventListener('click', () => {
      if (state.quran.speechTrackerActive) {
        stopTracker();
      } else {
        startTracker();
      }
    });
  }

  if (closeBtn) closeBtn.addEventListener('click', stopTracker);
  if (btnAdvance) btnAdvance.addEventListener('click', () => advanceRecitationToNext('تم الانتقال بالضغط المباشر ⏭'));

  // Target verse click interaction in Mushaf
  document.addEventListener('click', (e) => {
    if (!state.quran.speechTrackerActive) return;
    const vUnit = e.target.closest('.verse-unit');
    if (!vUnit) return;

    const s = parseInt(vUnit.getAttribute('data-sura'), 10);
    const a = parseInt(vUnit.getAttribute('data-aya'), 10);
    if (!s || !a) return;

    if (s === currentTargetSura && a === currentTargetAya) {
      // Clicking the target verse toggles peek
      togglePeekCurrentVerse();
    } else {
      // Clicking any other verse sets it as the new recitation target
      currentTargetSura = s;
      currentTargetAya = a;
      applyTargetVerseState(currentTargetSura, currentTargetAya, { reasonText: 'تم تحديد هذه الآية للتسميع 🎯' });
    }
  });

  // Keyboard shortcuts while in Voice Recitation mode
  window.addEventListener('keydown', (e) => {
    if (state.quran.speechTrackerActive && state.activeTab === 'quran') {
      if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;

      if (e.code === 'Space') {
        e.preventDefault();
        advanceRecitationToNext('تم الانتقال عبر مفتاح المسافة (Space) ⏭');
      } else if (e.code === 'KeyR' || e.code === 'KeyV') {
        e.preventDefault();
        togglePeekCurrentVerse();
      } else if (e.code === 'KeyL') {
        e.preventDefault();
        toggleAudioPreview();
      }
    }
  });
}

// =========================================================================
// Smart Hifz Tutor (المحفظ الآلي وتكرار الآيات واختبار الحفظ)
// =========================================================================
function setupHifzTutorDialog() {
  const modal = document.getElementById('modalHifzTutor');
  const openBtn = document.getElementById('btn-open-hifz-tutor');
  const closeBtn = document.getElementById('btnCloseHifzModal');
  const repeatBtns = document.querySelectorAll('#hifzRepeatButtons .d-tab-btn');
  const btnToggleWordHiding = document.getElementById('btnToggleWordHiding');
  const labelWordHiding = document.getElementById('labelWordHiding');
  const btnStartHifz = document.getElementById('btnStartHifzRecitation');

  if (!modal) return;

  function closeModal() {
    modal.classList.remove('open');
  }

  if (openBtn) openBtn.addEventListener('click', () => modal.classList.add('open'));
  if (closeBtn) closeBtn.addEventListener('click', closeModal);
  modal.addEventListener('click', (e) => {
    if (e.target === modal) closeModal();
  });

  // Repetition Buttons
  repeatBtns.forEach(b => {
    const val = parseInt(b.getAttribute('data-repeat'), 10);
    if (val === state.quran.hifzRepeatCount) {
      repeatBtns.forEach(x => x.classList.remove('active'));
      b.classList.add('active');
    }

    b.addEventListener('click', () => {
      repeatBtns.forEach(x => x.classList.remove('active'));
      b.classList.add('active');
      state.quran.hifzRepeatCount = val;
      state.quran.hifzCurrentIteration = 1;
      localStorage.setItem('sb_hifz_repeat', val);
      updateHifzRepeatBadge();
    });
  });

  // Word Hiding / Masking for Active Recall
  if (btnToggleWordHiding) {
    btnToggleWordHiding.addEventListener('click', () => {
      state.quran.wordHidingActive = !state.quran.wordHidingActive;
      applyWordHidingMode(state.quran.wordHidingActive);
      if (labelWordHiding) {
        labelWordHiding.textContent = state.quran.wordHidingActive 
          ? 'إلغاء إخفاء الكلمات (إظهار الكل)' 
          : 'تفعيل إخفاء الكلمات (انقر على الكلمة للكشف)';
      }
      btnToggleWordHiding.style.background = state.quran.wordHidingActive ? '#D4AF37' : '';
      btnToggleWordHiding.style.color = state.quran.wordHidingActive ? '#1E293B' : '';
    });
  }

  // Start Hifz recitation for current ayah
  if (btnStartHifz) {
    btnStartHifz.addEventListener('click', () => {
      closeModal();
      const curPage = state.quran.currentPageNum || 1;
      const pageAyahs = window.QURAN_PAGES[String(curPage)] || [];
      const firstA = pageAyahs.find(x => !x.isBasmala || x.aya > 0);
      if (firstA) {
        state.quran.hifzCurrentIteration = 1;
        playAyahRecitation(firstA.sura, firstA.aya);
      }
    });
  }
}

function applyWordHidingMode(enable) {
  document.querySelectorAll('.verse-unit').forEach(verseEl => {
    if (enable) {
      if (!verseEl.querySelector('.word-masked')) {
        const fullHtml = verseEl.innerHTML;
        const glyphMatch = fullHtml.match(/<span class="verse-symbol-glyph">[\s\S]*?<\/span>/);
        const glyphHtml = glyphMatch ? glyphMatch[0] : '';
        const rawText = verseEl.textContent.replace(/۝[٠-٩0-9]+/, '').trim();
        const words = rawText.split(/\s+/);
        const maskedHtml = words.map(w => {
          return `<span class="word-masked" onclick="this.classList.toggle('revealed')">${w}</span>`;
        }).join(' ') + ' ' + glyphHtml;
        verseEl.innerHTML = maskedHtml;
      }
    } else {
      verseEl.querySelectorAll('.word-masked').forEach(w => w.classList.remove('word-masked', 'revealed'));
    }
  });
}

// Global Quran & Islamic Encyclopedia Search Dialog (dialog_modern_search.xml)
function setupQuranSearchDialog() {
  const modal = document.getElementById('modalSearch');
  const closeBtn = document.getElementById('btnCloseSearch');
  const input = document.getElementById('inputQuranSearch');
  const resultsContainer = document.getElementById('searchResultsList');
  const scopeTabs = document.querySelectorAll('#searchScopeTabs .btn-search-tab-pill');

  if (!modal || !input || !resultsContainer) return;

  let currentScope = 'quran';

  function closeModal() {
    modal.classList.remove('open');
  }

  window.openGlobalSearchModal = function() {
    modal.classList.add('open');
    setTimeout(() => {
      input.focus();
      input.select();
    }, 120);
  };

  if (closeBtn) closeBtn.addEventListener('click', closeModal);
  modal.addEventListener('click', (e) => {
    if (e.target === modal) closeModal();
  });

  scopeTabs.forEach(tab => {
    tab.addEventListener('click', () => {
      scopeTabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      currentScope = tab.getAttribute('data-type') || 'quran';
      performSearch();
    });
  });

  input.addEventListener('input', () => {
    performSearch();
  });

  function performSearch() {
    const rawVal = input.value.trim();
    const q = typeof normalizeArabicText === 'function' ? normalizeArabicText(rawVal) : rawVal;

    if (q.length < 2) {
      resultsContainer.innerHTML = '<div style="color: #64748B; text-align: center; padding: 28px 16px; font-size: 14px;">اكتب كلمتين أو أكثر للبحث في الأقسام الإسلامية...</div>';
      return;
    }

    resultsContainer.innerHTML = '';

    if (currentScope === 'quran') {
      searchQuranScope(q, rawVal);
    } else if (currentScope === 'tafseer') {
      searchTafseerScope(q, rawVal);
    } else if (currentScope === 'hadith') {
      searchHadithScope(q, rawVal);
    } else if (currentScope === 'azkar') {
      searchAzkarScope(q, rawVal);
    }
  }

  // 1. Quran Scope
  function searchQuranScope(q, rawVal) {
    if (!window.QURAN_PAGES) return;
    const matches = [];
    const pageKeys = Object.keys(window.QURAN_PAGES);

    for (let p of pageKeys) {
      const pageAyahs = window.QURAN_PAGES[p];
      for (let a of pageAyahs) {
        if (a.isBasmala && a.aya === 0) continue;
        const normAyah = normalizeArabicText(a.text);
        if (normAyah.includes(q)) {
          matches.push({ ...a, pageNum: parseInt(p, 10) });
          if (matches.length >= 50) break;
        }
      }
      if (matches.length >= 50) break;
    }

    if (matches.length === 0) {
      renderNoResults();
      return;
    }

    matches.forEach(m => {
      const card = document.createElement('div');
      card.className = 'modern-search-item-card';
      card.innerHTML = `
        <div class="search-item-header">
          <span class="search-item-badge">📖 سورة ${m.sura_name} [آية ${m.aya}]</span>
          <span class="search-item-meta">صفحة ${m.pageNum} • الجزء ${m.juz || 1}</span>
        </div>
        <div class="search-item-text">${m.text}</div>
      `;
      card.addEventListener('click', () => {
        closeModal();
        if (window.switchTab) window.switchTab('quran');
        if (typeof jumpToQuranPage === 'function') {
          setTimeout(() => jumpToQuranPage(m.pageNum, m.aya), 150);
        }
      });
      resultsContainer.appendChild(card);
    });
  }

  // 2. Tafseer Scope
  function searchTafseerScope(q, rawVal) {
    if (!window.QURAN_PAGES) return;
    const matches = [];
    const pageKeys = Object.keys(window.QURAN_PAGES);

    for (let p of pageKeys) {
      const pageAyahs = window.QURAN_PAGES[p];
      for (let a of pageAyahs) {
        if (a.isBasmala && a.aya === 0) continue;
        const normAyah = normalizeArabicText(a.text);
        const normEn = (a.translation_en || '').toLowerCase();
        if (normAyah.includes(q) || normEn.includes(rawVal.toLowerCase())) {
          matches.push({ ...a, pageNum: parseInt(p, 10) });
          if (matches.length >= 40) break;
        }
      }
      if (matches.length >= 40) break;
    }

    if (matches.length === 0) {
      renderNoResults();
      return;
    }

    matches.forEach(m => {
      const card = document.createElement('div');
      card.className = 'modern-search-item-card';
      card.innerHTML = `
        <div class="search-item-header">
          <span class="search-item-badge">🔍 تفسير وترجمة: سورة ${m.sura_name} [${m.aya}]</span>
          <span class="search-item-meta">صفحة ${m.pageNum}</span>
        </div>
        <div class="search-item-text" style="font-size: 15px; margin-bottom: 4px;">${m.text}</div>
        ${m.translation_en ? `<div style="font-size: 13px; color: #64748B; font-style: italic;">${m.translation_en}</div>` : ''}
      `;
      card.addEventListener('click', () => {
        closeModal();
        if (window.switchTab) window.switchTab('quran');
        if (typeof jumpToQuranPage === 'function') {
          setTimeout(() => {
            jumpToQuranPage(m.pageNum, m.aya);
            if (typeof openVerseOptions === 'function') {
              openVerseOptions(m, m.pageNum, m.text);
            }
          }, 150);
        }
      });
      resultsContainer.appendChild(card);
    });
  }

  // 3. Hadith Scope
  function searchHadithScope(q, rawVal) {
    const matches = [];
    // If globalIndex is available in hadithState
    if (typeof hadithState !== 'undefined' && hadithState.globalIndex && hadithState.globalIndex.length > 0) {
      for (let i = 0; i < hadithState.globalIndex.length; i++) {
        const entry = hadithState.globalIndex[i];
        const bId = entry[0];
        const hNum = entry[1];
        const ch = entry[2];
        const snippet = entry[3];
        if (snippet.includes(q) || (ch && ch.includes(q))) {
          matches.push({ bId, hNum, ch, snippet });
          if (matches.length >= 40) break;
        }
      }
    } else if (typeof hadithState !== 'undefined' && hadithState.currentBookData && hadithState.currentBookData.length > 0) {
      // Fallback to active book
      hadithState.currentBookData.forEach(h => {
        const normH = normalizeArabicText(h.text);
        if (normH.includes(q)) {
          matches.push({
            bId: hadithState.currentBook ? hadithState.currentBook.id : 'bukhari',
            hNum: h.num,
            ch: h.ch,
            snippet: h.text
          });
        }
      });
    }

    if (matches.length === 0) {
      renderNoResults();
      return;
    }

    matches.forEach(m => {
      const book = typeof hadithState !== 'undefined' && hadithState.books ? hadithState.books.find(b => b.id === m.bId) : null;
      const bTitle = book ? book.title : 'كتاب الحديث';
      const card = document.createElement('div');
      card.className = 'modern-search-item-card';
      card.innerHTML = `
        <div class="search-item-header">
          <span class="search-item-badge">📜 ${bTitle} [حديث ${m.hNum}]</span>
          <span class="search-item-meta">${m.ch || 'باب عام'}</span>
        </div>
        <div class="search-item-text" style="font-size: 14.5px;">${m.snippet.substring(0, 220)}...</div>
      `;
      card.addEventListener('click', () => {
        closeModal();
        if (window.switchTab) window.switchTab('hadith');
        if (typeof openHadithBook === 'function') {
          setTimeout(() => openHadithBook(m.bId, m.hNum), 150);
        }
      });
      resultsContainer.appendChild(card);
    });
  }

  // 4. Azkar Scope
  function searchAzkarScope(q, rawVal) {
    if (!window.AZKAR_DATA || !window.AZKAR_DATA.items) {
      renderNoResults();
      return;
    }

    const matches = window.AZKAR_DATA.items.filter(item => {
      const normText = normalizeArabicText(item.text);
      const normTitle = normalizeArabicText(item.title || '');
      const normCat = normalizeArabicText(item.category || '');
      return normText.includes(q) || normTitle.includes(q) || normCat.includes(q);
    }).slice(0, 40);

    if (matches.length === 0) {
      renderNoResults();
      return;
    }

    matches.forEach(item => {
      const card = document.createElement('div');
      card.className = 'modern-search-item-card';
      card.innerHTML = `
        <div class="search-item-header">
          <span class="search-item-badge">📿 ${item.category}</span>
          <span class="search-item-meta">${item.count ? `العدد: ${item.count} مرات` : 'ذكر مبارك'}</span>
        </div>
        <div class="search-item-text" style="font-size: 15px;">${item.text}</div>
        ${item.virtues ? `<div style="font-size: 12.5px; color: var(--border-gold); margin-top: 4px;">✨ ${item.virtues}</div>` : ''}
      `;
      card.addEventListener('click', () => {
        closeModal();
        if (window.switchTab) window.switchTab('azkar');
        if (typeof azkarState !== 'undefined') {
          azkarState.activeGroup = 'الكل';
          azkarState.activeSubcategory = item.category;
          const sel = document.getElementById('selectAzkarSubcategory');
          if (sel) sel.value = item.category;
          if (typeof filterAndRenderAzkar === 'function') filterAndRenderAzkar();
        }
      });
      resultsContainer.appendChild(card);
    });
  }

  function renderNoResults() {
    resultsContainer.innerHTML = '<div style="color: #64748B; text-align: center; padding: 28px 16px; font-size: 14px;">لم يتم العثور على نتائج مطابقة في هذا القسم.</div>';
  }
}

// Review / Memorization Mode (وضع الحفظ والمراجعة كما في أندرويد)
function setupQuranReviewMode() {
  const btn = document.getElementById('btn-toggle-review');
  const roll = document.getElementById('quranRollContainer');
  if (!btn || !roll) return;

  btn.addEventListener('click', () => {
    state.quran.reviewMode = !state.quran.reviewMode;
    roll.classList.toggle('review-mode-active', state.quran.reviewMode);
    btn.style.background = state.quran.reviewMode ? 'var(--border-gold)' : 'transparent';
    btn.style.color = state.quran.reviewMode ? '#FFFFFF' : 'var(--text-dark)';
  });
}

// Resume Quran Bookmark Action & Set Reading Bookmark Action
function setupQuranResumeButton() {
  const btnResume = document.getElementById('btn-quran-resume');
  if (btnResume) {
    btnResume.addEventListener('click', () => {
      const savedPage = Math.max(1, Math.min(604, parseInt(localStorage.getItem('sb_last_page_num') || '1', 10)));
      const savedVerse = parseInt(localStorage.getItem('sb_last_verse_num') || '1', 10);
      const savedSurah = localStorage.getItem('sb_last_surah_name');
      jumpToQuranPage(savedPage, savedVerse > 0 ? savedVerse : null, true);
      if (typeof showAppToast === 'function') {
        showAppToast(`📖 تم الانتقال إلى موضع قراءتك: ${savedSurah ? 'سورة ' + savedSurah + ' ' : ''}(آية ${toArabicDigits(savedVerse)}) - صـ ${toArabicDigits(savedPage)}`, 'info');
      }
    });
  }

  const btnSetBookmark = document.getElementById('btn-quran-set-bookmark');
  if (btnSetBookmark) {
    btnSetBookmark.addEventListener('click', () => {
      const pageNum = state.quran.currentPageNum || 1;
      const verseNum = state.quran.currentVerseNum || 1;
      const pageAyahs = window.QURAN_PAGES ? window.QURAN_PAGES[String(pageNum)] || [] : [];
      let suraName = '';
      if (pageAyahs.length > 0) {
        const found = pageAyahs.find(a => a.aya === verseNum);
        suraName = (found ? found.sura_name : pageAyahs[0].sura_name) || '';
      }
      saveQuranProgress(pageNum, suraName, verseNum, true);
      updateBookmarkVisualRibbon(pageNum, verseNum);
      if (typeof showAppToast === 'function') {
        showAppToast(`🔖 تم تثبيت فاصلة القراءة بنجاح: سورة ${suraName} (آية ${toArabicDigits(verseNum)}) - صـ ${toArabicDigits(pageNum)}`, 'success');
      }
    });
  }
}

// --- 9. Enhanced Jewel Adhkar Sanctuary Engine (موسوعة الأذكار والأوراد النبوية الشاملة) ---
let azkarState = {
  data: null,
  activeGroup: 'الكل',
  activeSubcategory: 'all',
  searchQuery: '',
  soundEnabled: true,
  progress: {},
  favorites: [],
  displayLimit: 40,
  morningEveningMode: 'auto', // 'auto' | 'morning' | 'evening'
  curatedMorningEvening: []
};

// Determine if current local time is morning (between 3:00 AM and 3:30 PM / Fajr to Asr)
function isCurrentTimeMorning() {
  const hour = new Date().getHours();
  return hour >= 3 && hour < 15;
}

function getEffectiveMorningEveningMode() {
  if (azkarState.morningEveningMode === 'morning') return 'morning';
  if (azkarState.morningEveningMode === 'evening') return 'evening';
  return isCurrentTimeMorning() ? 'morning' : 'evening';
}

// Mirroring Android AzkarActivity.kt adjustZikrTextForTime exactly
function adjustZikrTextForTime(text, isMorning) {
  if (!text) return '';
  let adjusted = String(text);
  if (isMorning) {
    if (adjusted.includes('اللَّهُمَّ بِكَ أَمْسَيْنَا')) {
      return 'اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ.';
    }
    adjusted = adjusted.replace(/أَمْسَيْتُ/g, 'أَصْبَحْتُ');
    adjusted = adjusted.replace(/أَمْسَيْنَا/g, 'أَصْبَحْنَا');
    adjusted = adjusted.replace(/وَأَمْسَى/g, 'وَأَصْبَحَ');
    adjusted = adjusted.replace(/أَمْسَى/g, 'أَصْبَحَ');
    adjusted = adjusted.replace(/أمسيت/g, 'أصبحت');
    adjusted = adjusted.replace(/أمسينا/g, 'أصبحنا');
    adjusted = adjusted.replace(/أمسى/g, 'أصبح');
    adjusted = adjusted.replace(/اللَّيْلَةِ/g, 'الْيَوْمِ');
    adjusted = adjusted.replace(/هَذِهِ اللَّيْلَةِ/g, 'هَذَا الْيَوْمِ');
    adjusted = adjusted.replace(/هذه الليلة/g, 'هذا اليوم');
    adjusted = adjusted.replace(/ليلتنا/g, 'يومنا');
    adjusted = adjusted.replace(/امسى/g, 'اصبح');
    adjusted = adjusted.replace(/فَتْحَهَا، وَنَصْرَهَا، وَنُورَهَا، وَبَرَكَتَهَا، وَهُدَاهَا، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِيهَا وَشَرِّ مَا بَعْدَهَا/g, 'فَتْحَهُ، وَنَصْرَهُ، وَنُورَهُ، وَبَرَكَتَهُ، وَهُدَاهُ، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِيهِ وَشَرِّ مَا بَعْدَهُ');
  } else {
    if (adjusted.includes('اللَّهُمَّ بِكَ أَصْبَحْنَا')) {
      return 'اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ الْمَصِيرُ.';
    }
    adjusted = adjusted.replace(/أَصْبَحْتُ/g, 'أَمْسَيْتُ');
    adjusted = adjusted.replace(/أَصْبَحْنَا/g, 'أَمْسَيْنَا');
    adjusted = adjusted.replace(/وَأَصْبَحَ/g, 'وَأَمْسَى');
    adjusted = adjusted.replace(/أَصْبَحَ/g, 'أَمْسَى');
    adjusted = adjusted.replace(/أصبحت/g, 'أمسيت');
    adjusted = adjusted.replace(/أصبحنا/g, 'أمسينا');
    adjusted = adjusted.replace(/أصبح/g, 'أمسى');
    adjusted = adjusted.replace(/الْيَوْمِ/g, 'اللَّيْلَةِ');
    adjusted = adjusted.replace(/هَذَا الْيَوْمِ/g, 'هَذِهِ اللَّيْلَةِ');
    adjusted = adjusted.replace(/هذا اليوم/g, 'هذه الليلة');
    adjusted = adjusted.replace(/يومنا/g, 'ليلتنا');
    adjusted = adjusted.replace(/اصبح/g, 'امسى');
    adjusted = adjusted.replace(/فَتْحَهُ، وَنَصْرَهُ، وَنُورَهُ، وَبَرَكَتَهُ، وَهُدَاهُ، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِيهِ وَشَرِّ مَا بَعْدَهُ/g, 'فَتْحَهَا، وَنَصْرَهَا، وَنُورَهَا، وَبَرَكَتَهَا، وَهُدَاهَا، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِيهَا وَشَرِّ مَا بَعْدَهَا');
  }
  return adjusted;
}

// Arabic Normalization Helper
function normalizeArabicText(text) {
  if (!text) return '';
  let s = String(text);
  // Remove tashkeel & Quranic annotations
  s = s.replace(/[\u064B-\u065F\u0670\u06D6-\u06ED]/g, '');
  // Normalize alef
  s = s.replace(/[إأآا]/g, 'ا');
  // Normalize ta marbuta & alef maksura
  s = s.replace(/ة/g, 'ه').replace(/ى/g, 'ي');
  return s.replace(/\s+/g, ' ').trim().toLowerCase();
}

// Toast notification helper
function showAzkarToast(msg, icon = '✨') {
  let toast = document.getElementById('azkarToastNotification');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'azkarToastNotification';
    toast.className = 'azkar-toast-notification';
    document.body.appendChild(toast);
  }
  toast.innerHTML = `<span>${icon}</span><span>${msg}</span>`;
  toast.classList.add('show');
  clearTimeout(toast._timeout);
  toast._timeout = setTimeout(() => {
    toast.classList.remove('show');
  }, 2400);
}

async function loadAzkarData() {
  try {
    // 1. Synchronous bundle or fallback
    if (window.AZKAR_DATA && window.AZKAR_DATA.items) {
      azkarState.data = window.AZKAR_DATA;
    } else {
      const res = await fetch('data/azkar.json');
      azkarState.data = await res.json();
    }

    // 1.1 Load Morning & Evening Curated Dataset from Android
    if (window.AZKAR_MORNING_EVENING_DATA && Array.isArray(window.AZKAR_MORNING_EVENING_DATA)) {
      azkarState.curatedMorningEvening = window.AZKAR_MORNING_EVENING_DATA;
    } else {
      try {
        const resME = await fetch('data/azkar_morning_evening.json');
        azkarState.curatedMorningEvening = await resME.json();
      } catch (_) {}
    }

    state.azkar = azkarState.data;

    // 2. Load stored progress, favorites & sound setting
    try {
      azkarState.progress = JSON.parse(localStorage.getItem('sb_azkar_progress_v2') || '{}');
      azkarState.favorites = JSON.parse(localStorage.getItem('sb_azkar_favorites') || '[]');
      const storedSound = localStorage.getItem('sb_azkar_sound');
      if (storedSound !== null) azkarState.soundEnabled = storedSound === 'true';

      const savedGroup = localStorage.getItem('sb_azkar_active_group');
      if (savedGroup) azkarState.activeGroup = savedGroup;
      const savedSubcat = localStorage.getItem('sb_azkar_active_subcat');
      if (savedSubcat) azkarState.activeSubcategory = savedSubcat;
    } catch (_) {}

    // Update sound button UI
    updateAzkarSoundUI();

    // 3. Populate subcategory dropdown
    setupAzkarSubcategoriesDropdown();

    // 4. Populate Major Group Tabs
    setupAzkarMajorGroupTabs();

    // 5. Setup Search and Controls
    setupAzkarControls();

    // 5.1 Setup Dedicated Morning & Evening Hero Card UI
    setupMorningEveningHeroUI();

    // 6. Initial Render
    filterAndRenderAzkar();

  } catch (e) {
    console.error('Error loading azkar encyclopedia:', e);
  }
}

function updateAzkarSoundUI() {
  const icon = document.getElementById('azkarSoundIcon');
  const label = document.getElementById('azkarSoundLabel');
  if (icon) icon.textContent = azkarState.soundEnabled ? '🔊' : '🔇';
  if (label) label.textContent = azkarState.soundEnabled ? 'الصوت: مفعل' : 'الصوت: مكتوم';
}

function setupAzkarSubcategoriesDropdown() {
  const select = document.getElementById('selectAzkarSubcategory');
  if (!select || !azkarState.data || !azkarState.data.items) return;

  // Gather unique categories with counts
  const catMap = new Map();
  azkarState.data.items.forEach(it => {
    const c = it.category || 'عام';
    catMap.set(c, (catMap.get(c) || 0) + 1);
  });

  // Sort categories alphabetically or by size
  const sortedCats = Array.from(catMap.entries()).sort((a, b) => a[0].localeCompare(b[0], 'ar'));

  select.innerHTML = `<option value="all">كل الأبواب والتصنيفات (${sortedCats.length} باباً)</option>`;
  sortedCats.forEach(([cat, count]) => {
    const opt = document.createElement('option');
    opt.value = cat;
    opt.textContent = `${cat} (${count})`;
    select.appendChild(opt);
  });

  if (azkarState.activeSubcategory && azkarState.activeSubcategory !== 'all') {
    select.value = azkarState.activeSubcategory;
  }

  select.addEventListener('change', (e) => {
    azkarState.activeSubcategory = e.target.value;
    azkarState.displayLimit = 40;
    try {
      localStorage.setItem('sb_azkar_active_subcat', e.target.value);
    } catch (_) {}
    filterAndRenderAzkar();
  });
}

function setupAzkarMajorGroupTabs() {
  const bar = document.getElementById('azkarMajorGroupsBar');
  if (!bar || !azkarState.data) return;

  bar.innerHTML = '';

  // Defined Curated Major Hubs
  const majorTabs = [
    { id: 'الكل', name: 'الكل', icon: '🌟' },
    { id: 'أذكار الصباح والمساء', name: 'أذكار الصباح والمساء', icon: '🌅' },
    { id: 'أذكار الصباح', name: 'أذكار الصباح', icon: '☀️' },
    { id: 'أذكار المساء', name: 'أذكار المساء', icon: '🌙' },
    { id: 'أذكار الصلاة والمساجد والطهارة', name: 'الصلاة والمساجد', icon: '🕌' },
    { id: 'أذكار النوم والاستيقاظ', name: 'النوم والاستيقاظ', icon: '😴' },
    { id: 'تفريج الكروب والهموم والتحصين', name: 'تفريج الكروب والهموم', icon: '💚' },
    { id: 'أدعية قرآنية مباركة', name: 'أدعية قرآنية', icon: '📖' },
    { id: 'أدعية نبوية مأثورة وجوامع الكلم', name: 'أدعية نبوية', icon: '📿' },
    { id: 'أذكار السفر والخروج والتنقل', name: 'السفر والخروج', icon: '🚗' },
    { id: 'أذكار الطعام والشراب والمجالس', name: 'الطعام والمجالس', icon: '🍽️' },
    { id: 'المرض والشفاء والجنائز', name: 'المرض والشفاء', icon: '🤲' },
    { id: 'أدعية الحج والعمرة', name: 'الحج والعمرة', icon: '🕋' },
    { id: 'الآداب والمناسبات الجامعة', name: 'الآداب الجامعة', icon: '🌿' },
    { id: 'المفضلة', name: 'المفضلة', icon: '⭐' }
  ];

  majorTabs.forEach(tab => {
    const pill = document.createElement('button');
    pill.className = `azkar-group-pill ${tab.id === azkarState.activeGroup ? 'active' : ''}`;
    pill.dataset.group = tab.id;

    // Calculate count
    let count = 0;
    if (tab.id === 'الكل') {
      count = azkarState.data.items.length;
    } else if (tab.id === 'المفضلة') {
      count = azkarState.favorites.length;
    } else if (tab.id === 'أذكار الصباح والمساء') {
      count = (azkarState.curatedMorningEvening && azkarState.curatedMorningEvening.length > 0)
        ? azkarState.curatedMorningEvening.length
        : azkarState.data.items.filter(it => it.group === 'أذكار الصباح والمساء' || it.group === 'أذكار الصباح' || it.group === 'أذكار المساء').length;
    } else if (tab.id === 'أذكار الصباح') {
      count = (azkarState.curatedMorningEvening && azkarState.curatedMorningEvening.length > 0)
        ? azkarState.curatedMorningEvening.filter(it => it.timeScope !== 'evening').length
        : azkarState.data.items.filter(it => it.group === 'أذكار الصباح' || it.group === 'أذكار الصباح والمساء').length;
    } else if (tab.id === 'أذكار المساء') {
      count = (azkarState.curatedMorningEvening && azkarState.curatedMorningEvening.length > 0)
        ? azkarState.curatedMorningEvening.filter(it => it.timeScope !== 'morning').length
        : azkarState.data.items.filter(it => it.group === 'أذكار المساء' || it.group === 'أذكار الصباح والمساء').length;
    } else {
      count = azkarState.data.items.filter(it => it.group === tab.id).length;
    }

    pill.innerHTML = `<span>${tab.icon}</span> <span>${tab.name}</span> <span class="azkar-pill-count">${count}</span>`;

    pill.addEventListener('click', () => {
      document.querySelectorAll('#azkarMajorGroupsBar .azkar-group-pill').forEach(b => b.classList.remove('active'));
      pill.classList.add('active');
      azkarState.activeGroup = tab.id;
      azkarState.displayLimit = 40;
      try {
        localStorage.setItem('sb_azkar_active_group', tab.id);
      } catch (_) {}
      filterAndRenderAzkar();
    });

    bar.appendChild(pill);
  });
}

function setupAzkarControls() {
  const searchInput = document.getElementById('inputAzkarSearch');
  const btnClearSearch = document.getElementById('btnClearAzkarSearch');
  const btnResetFilter = document.getElementById('btnResetActiveFilter');
  const btnToggleSound = document.getElementById('btnToggleAzkarSound');
  const btnResetCounters = document.getElementById('btnResetAzkarCounters');

  if (searchInput) {
    let debounceTimer;
    searchInput.addEventListener('input', (e) => {
      clearTimeout(debounceTimer);
      const val = e.target.value.trim();
      if (btnClearSearch) btnClearSearch.style.display = val ? 'flex' : 'none';

      debounceTimer = setTimeout(() => {
        azkarState.searchQuery = val;
        azkarState.displayLimit = 40;
        filterAndRenderAzkar();
      }, 150);
    });
  }

  if (btnClearSearch) {
    btnClearSearch.addEventListener('click', () => {
      if (searchInput) {
        searchInput.value = '';
        searchInput.focus();
      }
      btnClearSearch.style.display = 'none';
      azkarState.searchQuery = '';
      azkarState.displayLimit = 40;
      filterAndRenderAzkar();
    });
  }

  if (btnResetFilter) {
    btnResetFilter.addEventListener('click', () => {
      azkarState.searchQuery = '';
      azkarState.activeSubcategory = 'all';
      azkarState.activeGroup = 'الكل';
      if (searchInput) searchInput.value = '';
      if (btnClearSearch) btnClearSearch.style.display = 'none';
      const select = document.getElementById('selectAzkarSubcategory');
      if (select) select.value = 'all';
      document.querySelectorAll('#azkarMajorGroupsBar .azkar-group-pill').forEach(p => {
        p.classList.toggle('active', p.dataset.group === 'الكل');
      });
      azkarState.displayLimit = 40;
      filterAndRenderAzkar();
    });
  }

  if (btnToggleSound) {
    btnToggleSound.addEventListener('click', () => {
      azkarState.soundEnabled = !azkarState.soundEnabled;
      localStorage.setItem('sb_azkar_sound', String(azkarState.soundEnabled));
      updateAzkarSoundUI();
      showAzkarToast(azkarState.soundEnabled ? 'تم تفعيل صوت نقر العداد' : 'تم كتم صوت نقر العداد', azkarState.soundEnabled ? '🔊' : '🔇');
    });
  }

  if (btnResetCounters) {
    btnResetCounters.addEventListener('click', () => {
      azkarState.progress = {};
      try {
        localStorage.removeItem('sb_azkar_progress_v2');
      } catch (_) {}
      filterAndRenderAzkar();
      showAzkarToast('تم تصفير جميع عدادات اليوم بنجاح للبدء من جديد', '↺');
    });
  }

  // Scroll tracking on panel-azkar to persist reading position
  const panelAzkar = document.getElementById('panel-azkar');
  if (panelAzkar) {
    let azkarScrollTimer = null;
    panelAzkar.addEventListener('scroll', () => {
      clearTimeout(azkarScrollTimer);
      azkarScrollTimer = setTimeout(() => {
        try {
          localStorage.setItem('sb_azkar_scroll_top', String(panelAzkar.scrollTop));
        } catch (_) {}
      }, 250);
    }, { passive: true });
  }
}

// Restore saved Azkar position and highlight last interacted item
function restoreAzkarProgressState() {
  const panelAzkar = document.getElementById('panel-azkar');
  const savedScroll = localStorage.getItem('sb_azkar_scroll_top');
  if (panelAzkar && savedScroll) {
    const sTop = parseInt(savedScroll, 10);
    if (!isNaN(sTop) && sTop > 0) {
      panelAzkar.scrollTop = sTop;
    }
  }

  const lastId = localStorage.getItem('sb_azkar_last_item_id');
  if (lastId) {
    const card = document.querySelector(`.azkar-jewel-card[data-id="${lastId}"]`);
    if (card) {
      card.classList.add('azkar-last-active-pulse');
      setTimeout(() => {
        card.classList.remove('azkar-last-active-pulse');
      }, 3500);
    }
  }
}
window.restoreAzkarProgressState = restoreAzkarProgressState;

function setupMorningEveningHeroUI() {
  const btnMorning = document.getElementById('btnModeMorning');
  const btnEvening = document.getElementById('btnModeEvening');
  const btnAuto = document.getElementById('btnModeAuto');
  const btnOpenFeed = document.getElementById('btnOpenMorningEveningFeed');
  const btnResetSession = document.getElementById('btnResetMeDailySession');

  // Load saved preference if any
  const savedMode = localStorage.getItem('sb_azkar_me_mode');
  if (savedMode === 'morning' || savedMode === 'evening' || savedMode === 'auto') {
    azkarState.morningEveningMode = savedMode;
  }

  function setMode(mode) {
    azkarState.morningEveningMode = mode;
    localStorage.setItem('sb_azkar_me_mode', mode);
    updateMorningEveningHeroUI();

    // If currently viewing Morning or Evening group, refresh rendering
    if (['أذكار الصباح والمساء', 'أذكار الصباح', 'أذكار المساء'].includes(azkarState.activeGroup)) {
      filterAndRenderAzkar();
    }
  }

  if (btnMorning) {
    btnMorning.addEventListener('click', () => setMode('morning'));
  }
  if (btnEvening) {
    btnEvening.addEventListener('click', () => setMode('evening'));
  }
  if (btnAuto) {
    btnAuto.addEventListener('click', () => setMode('auto'));
  }

  if (btnOpenFeed) {
    btnOpenFeed.addEventListener('click', () => {
      const effectiveMode = getEffectiveMorningEveningMode();
      const targetGroup = effectiveMode === 'morning' ? 'أذكار الصباح' : 'أذكار المساء';
      azkarState.activeGroup = targetGroup;
      azkarState.activeSubcategory = 'all';
      azkarState.searchQuery = '';
      azkarState.displayLimit = 40;

      // Update major group tabs
      document.querySelectorAll('#azkarMajorGroupsBar .azkar-group-pill').forEach(pill => {
        pill.classList.toggle('active', pill.dataset.group === targetGroup || pill.dataset.group === 'أذكار الصباح والمساء');
      });

      filterAndRenderAzkar();

      const pAzkar = document.getElementById('panel-azkar');
      if (pAzkar) {
        pAzkar.scrollTo({ top: 0, behavior: 'smooth' });
      }
    });
  }

  if (btnResetSession) {
    btnResetSession.addEventListener('click', () => {
      if (!azkarState.curatedMorningEvening) return;
      azkarState.curatedMorningEvening.forEach(item => {
        delete azkarState.progress[item.id];
      });
      try {
        localStorage.setItem('sb_azkar_progress_v2', JSON.stringify(azkarState.progress));
      } catch (_) {}
      updateMorningEveningHeroProgress();
      filterAndRenderAzkar();
      showAzkarToast('تم تصفير ورد الصباح والمساء لليوم بنجاح للبدء من جديد', '↺');
    });
  }

  updateMorningEveningHeroUI();
}

function updateMorningEveningHeroUI() {
  const mode = azkarState.morningEveningMode;
  const effective = getEffectiveMorningEveningMode();
  const isMorning = effective === 'morning';

  const btnMorning = document.getElementById('btnModeMorning');
  const btnEvening = document.getElementById('btnModeEvening');
  const btnAuto = document.getElementById('btnModeAuto');
  if (btnMorning) btnMorning.classList.toggle('active', mode === 'morning');
  if (btnEvening) btnEvening.classList.toggle('active', mode === 'evening');
  if (btnAuto) btnAuto.classList.toggle('active', mode === 'auto');

  const icon = document.getElementById('heroMeCurrentIcon');
  const timeStatus = document.getElementById('heroMeTimeStatus');
  const openLabel = document.getElementById('btnOpenMeFeedLabel');

  if (icon) icon.textContent = isMorning ? '☀️' : '🌙';

  if (timeStatus) {
    if (mode === 'auto') {
      timeStatus.textContent = isMorning
        ? 'الوضع التلقائي: وقت أذكار الصباح حالياً (الفجر حتى العصر) • تبديل الألفاظ الصباحية مفعل'
        : 'الوضع التلقائي: وقت أذكار المساء حالياً (العصر حتى الفجر) • تبديل الألفاظ المسائية مفعل';
    } else if (mode === 'morning') {
      timeStatus.textContent = 'الوضع اليدوي: ورد الصباح المختار • نصوص الصباح الثابتة مفعلة';
    } else {
      timeStatus.textContent = 'الوضع اليدوي: ورد المساء المختار • نصوص المساء الثابتة مفعلة';
    }
  }

  if (openLabel) {
    openLabel.textContent = isMorning
      ? 'عرض أذكار الصباح كاملة بالألفاظ الصباحية الصحيحة (من صحيح السنة)'
      : 'عرض أذكار المساء كاملة بالألفاظ المسائية الصحيحة (من صحيح السنة)';
  }

  updateMorningEveningHeroProgress();
}

function updateMorningEveningHeroProgress() {
  if (!azkarState.curatedMorningEvening || azkarState.curatedMorningEvening.length === 0) return;

  const effective = getEffectiveMorningEveningMode();
  const isMorning = effective === 'morning';

  const relevantItems = azkarState.curatedMorningEvening.filter(it => {
    if (isMorning && it.timeScope === 'evening') return false;
    if (!isMorning && it.timeScope === 'morning') return false;
    return true;
  });

  const total = relevantItems.length;
  let completed = 0;

  relevantItems.forEach(it => {
    const prog = azkarState.progress[it.id];
    if (prog === 0) completed++;
  });

  const percent = total > 0 ? Math.round((completed / total) * 100) : 0;

  const progressLabel = document.getElementById('heroMeProgressLabel');
  const percentLabel = document.getElementById('heroMePercentLabel');
  const progressFill = document.getElementById('heroMeProgressFill');

  if (progressLabel) {
    progressLabel.textContent = isMorning
      ? `إنجاز ورد الصباح: ${completed} من ${total} ذكراً مكتملاً`
      : `إنجاز ورد المساء: ${completed} من ${total} ذكراً مكتملاً`;
  }
  if (percentLabel) {
    percentLabel.textContent = `${percent}%`;
  }
  if (progressFill) {
    progressFill.style.width = `${percent}%`;
  }
}

function filterAndRenderAzkar() {
  if (!azkarState.data || !azkarState.data.items) return;

  const container = document.getElementById('azkarCardsContainer');
  const alertBox = document.getElementById('azkarActiveFilterAlert');
  const alertText = document.getElementById('azkarActiveFilterText');
  const statsLabel = document.getElementById('azkarTotalCountLabel');
  if (!container) return;

  const q = normalizeArabicText(azkarState.searchQuery);
  const grp = azkarState.activeGroup;
  const subcat = azkarState.activeSubcategory;

  const isMorningGroup = grp === 'أذكار الصباح';
  const isEveningGroup = grp === 'أذكار المساء';
  const isCombinedGroup = grp === 'أذكار الصباح والمساء';
  const isAnyMorningEvening = isMorningGroup || isEveningGroup || isCombinedGroup;

  let filtered = [];

  if (isAnyMorningEvening && subcat === 'all' && azkarState.curatedMorningEvening && azkarState.curatedMorningEvening.length > 0) {
    const effectiveMode = getEffectiveMorningEveningMode();
    const isMorning = isMorningGroup ? true : (isEveningGroup ? false : effectiveMode === 'morning');

    filtered = azkarState.curatedMorningEvening.filter(item => {
      // 1. TimeScope filter
      if (isMorning && item.timeScope === 'evening') return false;
      if (!isMorning && item.timeScope === 'morning') return false;

      // 2. Search Query filter
      if (q) {
        const adjusted = adjustZikrTextForTime(item.text, isMorning);
        const normText = normalizeArabicText(adjusted);
        const normTitle = normalizeArabicText(item.title);
        const normVirtues = normalizeArabicText(item.virtues);
        const normRef = normalizeArabicText(item.reference);
        return normText.includes(q) || normTitle.includes(q) || normVirtues.includes(q) || normRef.includes(q);
      }
      return true;
    }).map(item => ({
      ...item,
      category: isMorning ? 'أذكار الصباح (مسندة ومحققة)' : 'أذكار المساء (مسندة ومحققة)',
      text: adjustZikrTextForTime(item.text, isMorning)
    }));
  } else {
    filtered = azkarState.data.items.filter(item => {
      // 1. Group Filter
      if (grp === 'المفضلة') {
        if (!azkarState.favorites.includes(item.id)) return false;
      } else if (grp === 'أذكار الصباح والمساء') {
        if (item.group !== 'أذكار الصباح والمساء' && item.group !== 'أذكار الصباح' && item.group !== 'أذكار المساء') return false;
      } else if (grp === 'أذكار الصباح') {
        if (item.group !== 'أذكار الصباح' && item.group !== 'أذكار الصباح والمساء') return false;
      } else if (grp === 'أذكار المساء') {
        if (item.group !== 'أذكار المساء' && item.group !== 'أذكار الصباح والمساء') return false;
      } else if (grp !== 'الكل') {
        if (item.group !== grp) return false;
      }

      // 2. Subcategory Filter
      if (subcat !== 'all') {
        if (item.category !== subcat) return false;
      }

      // 3. Search Query Filter
      if (q) {
        const normText = normalizeArabicText(item.text);
        const normTitle = normalizeArabicText(item.title);
        const normCat = normalizeArabicText(item.category);
        const normVirtues = normalizeArabicText(item.virtues);
        const normRef = normalizeArabicText(item.reference);

        if (!normText.includes(q) && !normTitle.includes(q) && !normCat.includes(q) && !normVirtues.includes(q) && !normRef.includes(q)) {
          return false;
        }
      }

      return true;
    });
  }

  // Update Stats & Active Filter Banner
  if (statsLabel) {
    statsLabel.textContent = `${filtered.length} ذكراً مباركاً`;
  }

  const isFiltering = q || subcat !== 'all' || grp !== 'الكل';
  if (alertBox && alertText) {
    if (isFiltering) {
      alertBox.style.display = 'flex';
      let msg = 'عرض ';
      if (q) msg += `نتائج البحث عن: "${azkarState.searchQuery}" `;
      if (subcat !== 'all') msg += `في باب: [${subcat}] `;
      if (grp !== 'الكل') msg += `من قسم: [${grp}] `;
      msg += `(${filtered.length} نتيجة)`;
      alertText.textContent = msg;
    } else {
      alertBox.style.display = 'none';
    }
  }

  // Render cards
  container.innerHTML = '';

  if (filtered.length === 0) {
    container.innerHTML = `
      <div style="text-align: center; padding: 40px 20px; background: rgba(212, 175, 55, 0.08); border-radius: 16px; border: 1.5px dashed #D4AF37;">
        <span style="font-size: 32px; display: block; margin-bottom: 8px;">🔍</span>
        <h3 style="color: var(--text-primary); margin: 0 0 6px;">لم يتم العثور على أذكار مطابقة</h3>
        <p style="color: #64748B; font-size: 13px; margin: 0 0 14px;">جرب تغيير كلمة البحث أو اختيار باب آخر من الأبواب الـ ١٤٩</p>
        <button class="btn-azkar-aux" id="btnResetSearchEmpty" style="margin: 0 auto;">عرض كل الأذكار</button>
      </div>
    `;
    const btnResetEmpty = document.getElementById('btnResetSearchEmpty');
    if (btnResetEmpty) {
      btnResetEmpty.addEventListener('click', () => {
        const btnResetFilter = document.getElementById('btnResetActiveFilter');
        if (btnResetFilter) btnResetFilter.click();
      });
    }
    return;
  }

  // Centered Category Title Banner (عنوان متوسّط أنيق يوضح الباب الحالي)
  const feedHeader = document.createElement('div');
  feedHeader.className = 'azkar-feed-centered-category';

  let headerTitle = grp;
  if (subcat !== 'all') {
    headerTitle = subcat;
  } else if (q) {
    headerTitle = `نتائج البحث عن: "${azkarState.searchQuery}"`;
  } else if (isAnyMorningEvening && azkarState.curatedMorningEvening && azkarState.curatedMorningEvening.length > 0) {
    const eff = getEffectiveMorningEveningMode();
    const isM = grp === 'أذكار الصباح' ? true : (grp === 'أذكار المساء' ? false : eff === 'morning');
    headerTitle = isM ? 'أَذْكَارُ الصَّبَاحِ المُبَارَكَةِ (مِنْ صَحِيحِ السُّنَّةِ النَّبَوِيَّةِ)' : 'أَذْكَارُ المَسَاءِ المُبَارَكَةِ (مِنْ صَحِيحِ السُّنَّةِ النَّبَوِيَّةِ)';
  }

  feedHeader.innerHTML = `
    <div class="azkar-feed-title-line"></div>
    <div class="azkar-feed-title-content">
      <span class="azkar-feed-ornament">۞</span>
      <h3 class="azkar-feed-title-text">${headerTitle}</h3>
      <span class="azkar-feed-ornament">۞</span>
    </div>
    <div class="azkar-feed-title-line"></div>
  `;
  container.appendChild(feedHeader);

  // Slice for display limit (infinite scroll / load more)
  const toShow = filtered.slice(0, azkarState.displayLimit);

  const fragment = document.createDocumentFragment();

  toShow.forEach(item => {
    const card = createAzkarJewelCard(item, q);
    fragment.appendChild(card);
  });

  container.appendChild(fragment);

  // If there are more items to display, add load more trigger
  if (filtered.length > azkarState.displayLimit) {
    const loadMoreWrap = document.createElement('div');
    loadMoreWrap.style.cssText = 'display: flex; justify-content: center; padding: 14px 0 6px;';
    const btnLoadMore = document.createElement('button');
    btnLoadMore.className = 'btn-azkar-aux';
    btnLoadMore.style.cssText = 'padding: 8px 24px; font-size: 13px; font-weight: bold; background: linear-gradient(135deg, rgba(255,255,255,0.95), rgba(244,236,216,0.6)); border-color: #D4AF37;';
    btnLoadMore.textContent = `عرض المزيد من الأذكار (${filtered.length - azkarState.displayLimit} ذكر متبقٍ) ⬇`;
    btnLoadMore.addEventListener('click', () => {
      azkarState.displayLimit += 40;
      filterAndRenderAzkar();
    });
    loadMoreWrap.appendChild(btnLoadMore);
    container.appendChild(loadMoreWrap);
  }
}

function createAzkarJewelCard(item, rawQuery) {
  const card = document.createElement('div');
  card.className = 'azkar-jewel-card';
  card.dataset.id = item.id;

  const targetCount = item.count || 1;
  let remaining = azkarState.progress[item.id] !== undefined ? azkarState.progress[item.id] : targetCount;
  const isCompleted = remaining <= 0;

  if (isCompleted) {
    card.classList.add('completed');
  }

  const isFav = azkarState.favorites.includes(item.id);

  // Four ornate corner decorations
  const cornersHtml = `
    <div class="azkar-card-corner top-right"></div>
    <div class="azkar-card-corner top-left"></div>
    <div class="azkar-card-corner bottom-right"></div>
    <div class="azkar-card-corner bottom-left"></div>
  `;

  // Highlight helper
  function highlightText(str, query) {
    if (!str || !query) return str;
    const normQ = normalizeArabicText(query);
    if (!normQ) return str;
    // Replace if matched (case insensitive)
    const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
    return str.replace(regex, '<mark class="azkar-highlight">$1</mark>');
  }

  // Determine main title and subcategory display
  let mainTitle = item.title && item.title !== item.category ? item.title : item.category;
  let subCategoryDisp = item.category || 'أذكار مسندة';

  const virtuesBoxHtml = item.virtues ? `
    <div class="azkar-card-virtue-box">
      <div class="azkar-virtue-label">✨ فَضْلُ الذِّكْرِ وَأَثَرُهُ:</div>
      <div>${highlightText(item.virtues, rawQuery)}</div>
      ${item.reference ? `<div class="azkar-reference-tag">📜 ${highlightText(item.reference, rawQuery)}</div>` : ''}
    </div>
  ` : (item.reference ? `
    <div class="azkar-card-virtue-box">
      <div class="azkar-reference-tag">📜 تخريج الحديث: ${highlightText(item.reference, rawQuery)}</div>
    </div>
  ` : '');

  card.innerHTML = `
    ${cornersHtml}
    
    <div class="azkar-card-header">
      <div class="azkar-card-meta-left">
        <span class="azkar-chapter-badge">
          <span>۞</span>
          <span>${highlightText(subCategoryDisp, rawQuery)}</span>
        </span>
        <span class="azkar-target-badge">العدد: ${targetCount} ${targetCount === 1 ? 'مرة' : 'مرات'}</span>
      </div>

      <div class="azkar-card-actions-right">
        <button class="btn-card-action btn-fav ${isFav ? 'active' : ''}" title="${isFav ? 'إزالة من المفضلة' : 'إضافة للمفضلة'}">
          <span>${isFav ? '★' : '☆'}</span>
          <span>${isFav ? 'المفضلة' : 'حفظ'}</span>
        </button>
        <button class="btn-card-action btn-copy" title="نسخ الذكر مع التخريج">
          <span>📋</span>
          <span>نسخ</span>
        </button>
        <button class="btn-card-action btn-speak" title="الاستماع لنص الذكر">
          <span>🔊</span>
          <span>استماع</span>
        </button>
      </div>
    </div>

    <!-- Centered Dhikr Title (عنوان الذكر متوسّط في منتصف البطاقة) -->
    <div class="azkar-card-centered-title">
      <span class="azkar-title-ornament">۞</span>
      <h3 class="azkar-title-heading">${highlightText(mainTitle, rawQuery)}</h3>
      <span class="azkar-title-ornament">۞</span>
    </div>

    <div class="azkar-card-body">${highlightText(item.text, rawQuery)}</div>

    ${virtuesBoxHtml}

    <div class="azkar-card-footer">
      <button class="azkar-counter-interactive-btn ${isCompleted ? 'completed' : ''}">
        ${isCompleted ? '<span>✓</span> <span>تم بحمد الله</span>' : `<span>📿</span> <span>تبقى: ${remaining}</span>`}
      </button>

      <button class="btn-card-reset-count" title="تصفير عداد هذا الذكر">
        ↺
      </button>
    </div>
  `;

  // 1. Counter Button Action
  const counterBtn = card.querySelector('.azkar-counter-interactive-btn');
  const resetBtn = card.querySelector('.btn-card-reset-count');

  counterBtn.addEventListener('click', () => {
    try {
      localStorage.setItem('sb_azkar_last_item_id', String(item.id));
    } catch (_) {}

    if (azkarState.soundEnabled) {
      if (typeof audioEngine !== 'undefined' && audioEngine.playBeadClick) {
        audioEngine.playBeadClick();
      }
    }

    if (remaining > 1) {
      remaining--;
      azkarState.progress[item.id] = remaining;
      counterBtn.innerHTML = `<span>📿</span> <span>تبقى: ${remaining}</span>`;
      try {
        localStorage.setItem('sb_azkar_progress_v2', JSON.stringify(azkarState.progress));
      } catch (_) {}
      updateMorningEveningHeroProgress();
    } else if (remaining === 1) {
      remaining = 0;
      azkarState.progress[item.id] = 0;
      card.classList.add('completed');
      counterBtn.classList.add('completed');
      counterBtn.innerHTML = `<span>✓</span> <span>تم بحمد الله</span>`;
      try {
        localStorage.setItem('sb_azkar_progress_v2', JSON.stringify(azkarState.progress));
      } catch (_) {}
      updateMorningEveningHeroProgress();
      showAzkarToast(`أتممت ذكر: ${item.category} بحمد الله`, '🌟');
    } else {
      // Already completed, tap resets
      remaining = targetCount;
      azkarState.progress[item.id] = remaining;
      card.classList.remove('completed');
      counterBtn.classList.remove('completed');
      counterBtn.innerHTML = `<span>📿</span> <span>تبقى: ${remaining}</span>`;
      try {
        localStorage.setItem('sb_azkar_progress_v2', JSON.stringify(azkarState.progress));
      } catch (_) {}
      updateMorningEveningHeroProgress();
    }
  });

  // 2. Individual Reset Action
  resetBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    remaining = targetCount;
    delete azkarState.progress[item.id];
    card.classList.remove('completed');
    counterBtn.classList.remove('completed');
    counterBtn.innerHTML = `<span>📿</span> <span>تبقى: ${remaining}</span>`;
    try {
      localStorage.setItem('sb_azkar_progress_v2', JSON.stringify(azkarState.progress));
    } catch (_) {}
    updateMorningEveningHeroProgress();
  });

  // 3. Favorite Action
  const favBtn = card.querySelector('.btn-fav');
  favBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    const idx = azkarState.favorites.indexOf(item.id);
    if (idx >= 0) {
      azkarState.favorites.splice(idx, 1);
      favBtn.classList.remove('active');
      favBtn.innerHTML = `<span>☆</span><span>حفظ</span>`;
      showAzkarToast('تمت الإزالة من الأذكار المفضلة', '☆');
    } else {
      azkarState.favorites.push(item.id);
      favBtn.classList.add('active');
      favBtn.innerHTML = `<span>★</span><span>المفضلة</span>`;
      showAzkarToast('تمت الإضافة إلى الأذكار المفضلة بنجاح', '⭐');
    }
    try {
      localStorage.setItem('sb_azkar_favorites', JSON.stringify(azkarState.favorites));
    } catch (_) {}

    // Update pill count for favorites
    const favPill = document.querySelector('#azkarMajorGroupsBar .azkar-group-pill[data-group="المفضلة"] .azkar-pill-count');
    if (favPill) favPill.textContent = azkarState.favorites.length;
  });

  // 4. Copy Action
  const copyBtn = card.querySelector('.btn-copy');
  copyBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    let textToCopy = `${item.title || item.category}\n\n${item.text}`;
    if (item.virtues) textToCopy += `\n\nفضل الذكر: ${item.virtues}`;
    if (item.reference) textToCopy += `\n[${item.reference}]`;
    textToCopy += `\n\n— تطبيق سبح بخشوع`;

    navigator.clipboard.writeText(textToCopy).then(() => {
      showAzkarToast('تم نسخ الذكر وتخريجه إلى الحافظة بنجاح', '📋');
    }).catch(() => {
      showAzkarToast('تعذر النسخ إلى الحافظة', '⚠️');
    });
  });

  // 5. Speech Synthesizer Action
  const speakBtn = card.querySelector('.btn-speak');
  speakBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    if (!('speechSynthesis' in window)) {
      showAzkarToast('خاصية النطق الصوتي غير مدعومة في نظامك', 'ℹ️');
      return;
    }
    if (window.speechSynthesis.speaking) {
      window.speechSynthesis.cancel();
      speakBtn.innerHTML = `<span>🔊</span><span>استماع</span>`;
      return;
    }

    const cleanText = item.text.replace(/[\(\)\[\]۝0-9]/g, '');
    const utterance = new SpeechSynthesisUtterance(cleanText);
    utterance.lang = 'ar-SA';
    utterance.rate = 0.9;

    speakBtn.innerHTML = `<span>⏹</span><span>إيقاف</span>`;

    utterance.onend = () => {
      speakBtn.innerHTML = `<span>🔊</span><span>استماع</span>`;
    };
    utterance.onerror = () => {
      speakBtn.innerHTML = `<span>🔊</span><span>استماع</span>`;
    };

    window.speechSynthesis.speak(utterance);
    showAzkarToast('جاري قراءة نص الذكر بصوت مسموع...', '🔊');
  });

  return card;
}

// =========================================================================
// 10. Comprehensive Prophetic Hadith Encyclopedia (موسوعة الأحاديث النبوية الشريفة)
// قراءة وتصفح في ١٧ كتاباً معتمداً • بحث مفرد داخل الكتاب • بحث شامل في ٥٠,٨٨٤ حديثاً
// =========================================================================

const hadithState = {
  books: [],              // 17 authentic books metadata from books_index.json
  currentBook: null,      // Active book metadata
  currentBookData: [],    // Full hadiths array of active book
  currentFilteredData: [],// Filtered items (by chapter or search query)
  activeCategory: 'الكل', // 'الكل', 'الصحيحان', 'الأربعينيات والقدسيات', 'الآداب والشمائل', 'السنن والمسانيد', '⭐ المفضلة'
  activeScope: 'book',    // 'book' or 'global'
  activeChapter: 'all',   // 'all' or chapter title
  searchQuery: '',        // Search query string
  showEnglish: localStorage.getItem('sb_hadith_show_en') === 'true',
  fontSize: parseFloat(localStorage.getItem('sb_hadith_font_size') || '19.5'),
  favorites: JSON.parse(localStorage.getItem('sb_hadith_favorites') || '[]'),
  renderLimit: 30,        // Chunk size for infinite scroll / pagination
  globalIndex: null,      // Compact index of 50,884 hadiths for instant global search
  isGlobalLoading: false,
  isRestoringProgress: false // Lock to prevent feed scroll listener from overwriting restored progress
};

// Helper: Show notification toast
function showHadithToast(msg, icon = '📖') {
  if (typeof showAzkarToast === 'function') {
    showAzkarToast(msg, icon);
  }
}

// Helper: Build diacritic-tolerant Arabic RegEx for keyword highlighting
function buildHadithHighlightRegex(query) {
  if (!query || !query.trim()) return null;
  const cleanQ = query.trim().replace(/[.,\/#!$%\^&\*;:{}=\-_`~()]/g, '');
  if (!cleanQ) return null;
  const words = cleanQ.split(/\s+/).filter(w => w.length > 0);
  if (!words.length) return null;

  const patterns = words.map(w => {
    const chars = w.split('').map(c => {
      if (/[إأآا]/i.test(c)) return '[إأآا\u0671]';
      if (/[ىي]/i.test(c)) return '[ىي\u064A\u0649]';
      if (/[ةه]/i.test(c)) return '[ةه]';
      return c;
    });
    return chars.join('[\u064B-\u065F\u0670\u0640]*');
  });

  try {
    return new RegExp('(' + patterns.join('|') + ')', 'gi');
  } catch (e) {
    return null;
  }
}

// 1. Core Loader & Event Setup
async function loadHadithData() {
  try {
    const res = await fetch('data/hadiths/books_index.json');
    if (!res.ok) throw new Error(`HTTP error ${res.status}`);
    hadithState.books = await res.json();

    setupHadithCategoryPills();
    setupHadithControls();
    renderHadithShelf();

    // Preload global search index lazily in background after 1.5 seconds
    setTimeout(() => {
      preloadHadithGlobalIndex();
    }, 1500);

  } catch (e) {
    console.error('Error loading Hadith index:', e);
    const shelf = document.getElementById('hadithShelfGrid');
    if (shelf) {
      shelf.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 40px; background: rgba(255,255,255,0.85); border-radius: 20px; border: 1.5px solid var(--border-gold);">
          <div style="font-size: 38px; margin-bottom: 12px;">⚠️</div>
          <h3 style="color: var(--text-dark); margin-bottom: 8px;">تعذر تحميل فهرس كتب الحديث النبوي</h3>
          <p style="color: var(--text-muted); font-size: 14px;">يرجى التأكد من مسار البيانات المحلية وإعادة المحاولة.</p>
          <button class="btn-hadith-scope active" onclick="loadHadithData()" style="margin-top: 14px; padding: 8px 24px;">إعادة المحاولة ↺</button>
        </div>
      `;
    }
  }
}

// Preload global search index quietly
async function preloadHadithGlobalIndex() {
  if (hadithState.globalIndex || hadithState.isGlobalLoading) return;
  try {
    hadithState.isGlobalLoading = true;
    const res = await fetch('data/hadiths/search_index.json');
    if (res.ok) {
      hadithState.globalIndex = await res.json();
    }
  } catch (e) {
    console.warn('Could not preload hadith global index:', e);
  } finally {
    hadithState.isGlobalLoading = false;
  }
}

// 2. Setup Category Pills Bar
function setupHadithCategoryPills() {
  const bar = document.getElementById('hadithCategoriesBar');
  if (!bar) return;

  const categories = [
    { id: 'الكل', label: 'كل الكتب (١٧ كتاباً)', icon: '📚' },
    { id: 'الصحيحان', label: 'الصحيحان (البخاري ومسلم)', icon: '👑' },
    { id: 'الأربعينيات والقدسيات', label: 'الأربعينيات والقدسيات', icon: '💎' },
    { id: 'الآداب والشمائل', label: 'الآداب والشمائل النبوية', icon: '🌿' },
    { id: 'السنن والمسانيد', label: 'السنن والمسانيد والموطأ', icon: '📜' },
    { id: '⭐ المفضلة', label: `⭐ المفضلة (${hadithState.favorites.length})`, icon: '⭐' }
  ];

  bar.innerHTML = categories.map(cat => `
    <button class="btn-hadith-cat-pill ${cat.id === hadithState.activeCategory ? 'active' : ''}" data-cat="${cat.id}">
      <span>${cat.icon}</span>
      <span>${cat.label}</span>
    </button>
  `).join('');

  bar.querySelectorAll('.btn-hadith-cat-pill').forEach(btn => {
    btn.addEventListener('click', () => {
      const cat = btn.getAttribute('data-cat');
      hadithState.activeCategory = cat;
      bar.querySelectorAll('.btn-hadith-cat-pill').forEach(b => b.classList.toggle('active', b.getAttribute('data-cat') === cat));
      
      // If currently in reading or global search, return to shelf
      showHadithShelfStage();
      renderHadithShelf();
    });
  });
}

// 3. Setup Controls, Toolbar, and Search Listeners
function setupHadithControls() {
  const searchInput = document.getElementById('inputHadithSearch');
  const clearSearchBtn = document.getElementById('btnClearHadithSearch');
  const btnScopeBook = document.getElementById('btnScopeBook');
  const btnScopeGlobal = document.getElementById('btnScopeGlobal');
  const crumbShelf = document.getElementById('crumbShelf');
  const btnBackToShelf = document.getElementById('btnBackToShelf');
  const btnResetFilter = document.getElementById('btnResetActiveHadithFilter');
  const selectChapter = document.getElementById('selectHadithChapter');
  const jumpInput = document.getElementById('inputJumpHadithNum');
  const jumpBtn = document.getElementById('btnJumpHadith');
  const fontDownBtn = document.getElementById('btnHadithFontDown');
  const fontUpBtn = document.getElementById('btnHadithFontUp');
  const fontLabel = document.getElementById('hadithFontSizeLabel');
  const btnToggleEn = document.getElementById('btnToggleEnglishTranslation');
  const btnCloseGlobal = document.getElementById('btnCloseGlobalSearch');

  // Search scope toggle
  function setScope(scope) {
    hadithState.activeScope = scope;
    if (btnScopeBook) btnScopeBook.classList.toggle('active', scope === 'book');
    if (btnScopeGlobal) btnScopeGlobal.classList.toggle('active', scope === 'global');

    if (hadithState.searchQuery.trim()) {
      executeHadithSearch();
    }
  }

  if (btnScopeBook) btnScopeBook.addEventListener('click', () => setScope('book'));
  if (btnScopeGlobal) btnScopeGlobal.addEventListener('click', () => setScope('global'));

  // Search input debounce
  let searchTimer = null;
  if (searchInput) {
    searchInput.addEventListener('input', () => {
      const val = searchInput.value;
      hadithState.searchQuery = val;
      if (clearSearchBtn) clearSearchBtn.style.display = val ? 'block' : 'none';

      clearTimeout(searchTimer);
      searchTimer = setTimeout(() => {
        executeHadithSearch();
      }, 250);
    });

    searchInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        clearTimeout(searchTimer);
        executeHadithSearch();
      }
    });
  }

  if (clearSearchBtn) {
    clearSearchBtn.addEventListener('click', () => {
      searchInput.value = '';
      hadithState.searchQuery = '';
      clearSearchBtn.style.display = 'none';
      executeHadithSearch();
    });
  }

  if (btnResetFilter) {
    btnResetFilter.addEventListener('click', () => {
      if (searchInput) searchInput.value = '';
      hadithState.searchQuery = '';
      hadithState.activeChapter = 'all';
      if (clearSearchBtn) clearSearchBtn.style.display = 'none';
      if (selectChapter) selectChapter.value = 'all';
      executeHadithSearch();
    });
  }

  // Navigation Breadcrumbs & Back buttons
  if (crumbShelf) crumbShelf.addEventListener('click', showHadithShelfStage);
  if (btnBackToShelf) btnBackToShelf.addEventListener('click', showHadithShelfStage);
  if (btnCloseGlobal) btnCloseGlobal.addEventListener('click', showHadithShelfStage);

  // Chapter select change
  if (selectChapter) {
    selectChapter.addEventListener('change', () => {
      hadithState.activeChapter = selectChapter.value;
      hadithState.renderLimit = 30;
      applyBookFilters();
      renderHadithFeed();
    });
  }

  // Jump to Hadith Number
  function doJump() {
    if (!jumpInput) return;
    const num = parseInt(jumpInput.value.trim(), 10);
    if (!num || isNaN(num) || num <= 0) {
      showHadithToast('يرجى إدخال رقم حديث صحيح', '⚠️');
      return;
    }
    jumpToHadithNumber(num);
  }

  if (jumpBtn) jumpBtn.addEventListener('click', doJump);
  if (jumpInput) {
    jumpInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') doJump();
    });
  }

  // Font size adjustments
  if (fontLabel) fontLabel.textContent = `${Math.round(hadithState.fontSize)}px`;

  if (fontDownBtn) {
    fontDownBtn.addEventListener('click', () => {
      if (hadithState.fontSize > 15) {
        hadithState.fontSize -= 1.5;
        saveFontSize();
      }
    });
  }

  if (fontUpBtn) {
    fontUpBtn.addEventListener('click', () => {
      if (hadithState.fontSize < 32) {
        hadithState.fontSize += 1.5;
        saveFontSize();
      }
    });
  }

  function saveFontSize() {
    try {
      localStorage.setItem('sb_hadith_font_size', hadithState.fontSize);
    } catch (e) {}
    if (fontLabel) fontLabel.textContent = `${Math.round(hadithState.fontSize)}px`;
    document.querySelectorAll('.hadith-card-arabic').forEach(el => {
      el.style.fontSize = `${hadithState.fontSize}px`;
    });
  }

  // English translation toggle
  updateEnglishToggleUI();
  if (btnToggleEn) {
    btnToggleEn.addEventListener('click', () => {
      hadithState.showEnglish = !hadithState.showEnglish;
      try {
        localStorage.setItem('sb_hadith_show_en', hadithState.showEnglish);
      } catch (e) {}
      updateEnglishToggleUI();
      document.querySelectorAll('.hadith-card-english').forEach(el => {
        el.style.display = hadithState.showEnglish ? 'block' : 'none';
      });
      showHadithToast(hadithState.showEnglish ? 'تم إظهار الترجمة الإنجليزية' : 'تم إخفاء الترجمة الإنجليزية', '🇬🇧');
    });
  }

  function updateEnglishToggleUI() {
    const enIcon = document.getElementById('hadithEnIcon');
    const enLabel = document.getElementById('hadithEnLabel');
    if (btnToggleEn) {
      btnToggleEn.classList.toggle('active', hadithState.showEnglish);
    }
    if (enLabel) {
      enLabel.textContent = hadithState.showEnglish ? 'الترجمة: ظاهرة' : 'الترجمة: مخفية';
    }
  }

  // Scroll listener on hadithCardsFeed for seamless infinite scroll and reading progress tracking
  const feed = document.getElementById('hadithCardsFeed');
  if (feed) {
    let hadithScrollSaveTimer = null;
    feed.addEventListener('scroll', () => {
      if (feed.scrollTop + feed.clientHeight >= feed.scrollHeight - 300) {
        appendMoreHadiths();
      }

      if (hadithState.isRestoringProgress || !hadithState.currentBook) return;

      clearTimeout(hadithScrollSaveTimer);
      hadithScrollSaveTimer = setTimeout(() => {
        if (hadithState.isRestoringProgress || !hadithState.currentBook) return;
        const cards = feed.querySelectorAll('.hadith-jewel-card');
        const feedRect = feed.getBoundingClientRect();
        const readingLine = feedRect.top + 80;

        for (const c of cards) {
          const cr = c.getBoundingClientRect();
          if (cr.top <= readingLine && cr.bottom > readingLine) {
            const hNum = parseInt(c.id.replace('hadith-card-', ''), 10);
            if (hNum && hNum > 0) {
              try {
                localStorage.setItem(`sb_book_progress_${hadithState.currentBook.id}`, JSON.stringify({
                  hadithNum: hNum,
                  timestamp: Date.now()
                }));
              } catch (_) {}
            }
            break;
          }
        }
      }, 350);
    }, { passive: true });
  }
}

// 4. View Transitions (Shelf vs Reading vs Global Search)
function showHadithShelfStage() {
  const shelf = document.getElementById('hadithShelfGrid');
  const reading = document.getElementById('hadithReadingStage');
  const global = document.getElementById('hadithGlobalSearchStage');
  const crumbSep = document.getElementById('crumbSep');
  const crumbBook = document.getElementById('crumbBookTitle');
  const filterAlert = document.getElementById('hadithActiveFilterAlert');

  if (shelf) shelf.style.display = 'grid';
  if (reading) reading.style.display = 'none';
  if (global) global.style.display = 'none';

  if (crumbSep) crumbSep.style.display = 'none';
  if (crumbBook) crumbBook.style.display = 'none';
  if (filterAlert) filterAlert.style.display = 'none';

  const stats = document.getElementById('hadithStatsLabel');
  if (stats) stats.textContent = '١٧ كتاباً • ٥٠,٨٨٤ حديثاً شريفاً';

  // Automatically refresh shelf badges with latest saved reading progress
  renderHadithShelf();
}

function showHadithReadingStage() {
  const shelf = document.getElementById('hadithShelfGrid');
  const reading = document.getElementById('hadithReadingStage');
  const global = document.getElementById('hadithGlobalSearchStage');
  const crumbSep = document.getElementById('crumbSep');
  const crumbBook = document.getElementById('crumbBookTitle');

  if (shelf) shelf.style.display = 'none';
  if (reading) reading.style.display = 'block';
  if (global) global.style.display = 'none';

  if (crumbSep) crumbSep.style.display = 'inline';
  if (crumbBook) {
    crumbBook.style.display = 'inline';
    crumbBook.textContent = hadithState.currentBook ? hadithState.currentBook.title : '';
  }
}

function showHadithGlobalSearchStage() {
  const shelf = document.getElementById('hadithShelfGrid');
  const reading = document.getElementById('hadithReadingStage');
  const global = document.getElementById('hadithGlobalSearchStage');
  const crumbSep = document.getElementById('crumbSep');
  const crumbBook = document.getElementById('crumbBookTitle');

  if (shelf) shelf.style.display = 'none';
  if (reading) reading.style.display = 'none';
  if (global) global.style.display = 'block';

  if (crumbSep) crumbSep.style.display = 'inline';
  if (crumbBook) {
    crumbBook.style.display = 'inline';
    crumbBook.textContent = `نتائج البحث الشامل: "${hadithState.searchQuery}"`;
  }
}

// 5. Render Books Shelf
function renderHadithShelf() {
  const shelf = document.getElementById('hadithShelfGrid');
  if (!shelf) return;
  shelf.innerHTML = '';

  // If "Favorites" category is selected
  if (hadithState.activeCategory === '⭐ المفضلة') {
    renderFavoritesShelf(shelf);
    return;
  }

  // Filter books by category
  let filteredBooks = hadithState.books;
  if (hadithState.activeCategory !== 'الكل') {
    filteredBooks = hadithState.books.filter(b => b.category === hadithState.activeCategory);
  }

  // If single-book search query is entered while on shelf, filter books by title
  if (hadithState.searchQuery.trim() && hadithState.activeScope === 'book') {
    const qNorm = typeof normalizeArabicText === 'function' ? normalizeArabicText(hadithState.searchQuery) : hadithState.searchQuery.trim();
    filteredBooks = filteredBooks.filter(b => {
      const tNorm = typeof normalizeArabicText === 'function' ? normalizeArabicText(b.title) : b.title;
      const aNorm = typeof normalizeArabicText === 'function' ? normalizeArabicText(b.author) : b.author;
      return tNorm.includes(qNorm) || aNorm.includes(qNorm) || (b.title_en && b.title_en.toLowerCase().includes(hadithState.searchQuery.toLowerCase()));
    });
  }

  if (filteredBooks.length === 0) {
    shelf.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; padding: 48px 24px; background: rgba(255,255,255,0.85); border-radius: 20px; border: 1.5px dashed var(--border-gold);">
        <div style="font-size: 42px; margin-bottom: 12px;">🔍</div>
        <h3 style="color: var(--text-dark); margin-bottom: 8px;">لم يتم العثور على كتب مطابقة</h3>
        <p style="color: var(--text-muted); font-size: 14px;">جرّب التبديل إلى "بحث شامل في كل الكتب" للبحث في المتون والأسانيد الـ ٥٠,٨٨٤.</p>
        <button class="btn-hadith-scope active" onclick="hadithState.activeScope='global'; executeHadithSearch();" style="margin-top: 14px; padding: 8px 24px;">
          <span>🌐</span>
          <span>البحث الشامل في كل متون السنة</span>
        </button>
      </div>
    `;
    return;
  }

  filteredBooks.forEach(book => {
    const card = document.createElement('div');
    card.className = 'hadith-book-card';
    card.setAttribute('data-book-id', book.id);

    const chapterCount = book.chapters ? book.chapters.length : 0;
    const countFmt = book.count.toLocaleString('ar-SA');
    const chFmt = chapterCount.toLocaleString('ar-SA');

    let savedBadgeHtml = '';
    try {
      const savedRaw = localStorage.getItem(`sb_book_progress_${book.id}`);
      if (savedRaw) {
        const saved = JSON.parse(savedRaw);
        if (saved && saved.hadithNum) {
          savedBadgeHtml = `
            <div class="book-progress-saved-badge" style="display: inline-flex; align-items: center; gap: 4px; font-size: 11px; padding: 2px 8px; border-radius: 12px; background: rgba(16,185,129,0.15); color: var(--emerald-primary); border: 1px solid rgba(16,185,129,0.3); font-weight: bold; margin-bottom: 6px;">
              <span>🔖</span>
              <span>موضع القراءة: حديث (${saved.hadithNum.toLocaleString('ar-SA')})</span>
            </div>
          `;
        }
      }
    } catch (_) {}

    card.innerHTML = `
      <div class="book-card-gilded-spine">
        <span class="spine-dot">۞</span>
      </div>
      <div class="book-card-content">
        <div class="book-card-header">
          <div class="book-icon-badge">${book.icon || '📖'}</div>
          <div class="book-category-tag">${book.category}</div>
          <div class="book-count-badge">${countFmt} حَدِيثاً</div>
        </div>

        <h3 class="book-title-ar">${book.title}</h3>
        <div class="book-title-en">${book.title_en || ''}</div>
        <div class="book-author">${book.author}</div>
        ${savedBadgeHtml}
        <p class="book-desc">${book.description || ''}</p>

        <div class="book-card-footer">
          <div class="book-chapters-chip">
            <span>📑</span>
            <span>${chapterCount > 1 ? `${chFmt} باباً` : 'كتاب جامع'}</span>
          </div>
          <button class="btn-open-hadith-book">
            <span>${savedBadgeHtml ? 'متابعة القراءة' : 'تصفح وقراءة'}</span>
            <span>←</span>
          </button>
        </div>
      </div>
    `;

    card.addEventListener('click', () => {
      openHadithBook(book.id);
    });

    shelf.appendChild(card);
  });
}

// Render Favorites view inside shelf grid
function renderFavoritesShelf(container) {
  if (hadithState.favorites.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; padding: 50px 20px; background: rgba(255,255,255,0.9); border-radius: 20px; border: 1.5px dashed var(--border-gold);">
        <div style="font-size: 48px; margin-bottom: 14px;">⭐</div>
        <h3 style="color: var(--text-dark); margin-bottom: 8px;">قائمة الأحاديث المفضلة فارغة حالياً</h3>
        <p style="color: var(--text-muted); font-size: 14px; max-width: 520px; margin: 0 auto 16px; line-height: 1.7;">
          يمكنك إضافة أي حديث نبوي شريف إلى قائمتك المفضلة بالضغط على أيقونة النجمة ⭐ أعلى بطاقة الحديث للرجوع إليه وتدبره في أي وقت.
        </p>
        <button class="btn-hadith-cat-pill active" onclick="hadithState.activeCategory='الكل'; setupHadithCategoryPills(); renderHadithShelf();" style="display: inline-flex;">
          <span>📚</span>
          <span>تصفح كتب الحديث الآن</span>
        </button>
      </div>
    `;
    return;
  }

  container.innerHTML = `
    <div style="grid-column: 1 / -1; margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center;">
      <h3 style="color: var(--emerald-primary); font-size: 18px; margin: 0;">الأحاديث النبوية المفضلة لديك (${hadithState.favorites.length})</h3>
      <button class="btn-hadith-reset-filter" onclick="hadithState.favorites=[]; localStorage.removeItem('sb_hadith_favorites'); setupHadithCategoryPills(); renderHadithShelf(); showHadithToast('تم مسح قائمة المفضلة', '🗑️');">مسح المفضلة</button>
    </div>
  `;

  hadithState.favorites.forEach(item => {
    const card = createHadithCard(item, false, true);
    container.appendChild(card);
  });
}

// 6. Open Book & Fetch Hadiths On Demand
async function openHadithBook(bookId, targetHadithNum = null) {
  const book = hadithState.books.find(b => b.id === bookId);
  if (!book) return;

  hadithState.currentBook = book;
  hadithState.activeChapter = 'all';
  hadithState.renderLimit = 30;

  // Update Toolbar Header
  const nameEl = document.getElementById('readingBookName');
  const authorEl = document.getElementById('readingBookAuthor');
  const iconEl = document.getElementById('readingBookIcon');
  const selectCh = document.getElementById('selectHadithChapter');
  const statsEl = document.getElementById('hadithStatsLabel');

  if (nameEl) nameEl.textContent = book.title;
  if (authorEl) authorEl.textContent = book.author;
  if (iconEl) iconEl.textContent = book.icon || '📖';
  if (statsEl) statsEl.textContent = `${book.title} • ${book.count.toLocaleString('ar-SA')} حديثاً`;

  // Populate Chapters Dropdown
  if (selectCh) {
    selectCh.innerHTML = `<option value="all">كل الأبواب (${book.count.toLocaleString('ar-SA')} حديثاً)</option>`;
    if (book.chapters && book.chapters.length) {
      book.chapters.forEach(ch => {
        const opt = document.createElement('option');
        opt.value = ch.title;
        opt.textContent = `${ch.title} (${ch.count.toLocaleString('ar-SA')})`;
        selectCh.appendChild(opt);
      });
    }
    selectCh.value = 'all';
  }

  showHadithReadingStage();

  // Show Loading Spinner
  const feed = document.getElementById('hadithCardsFeed');
  if (feed) {
    feed.innerHTML = `
      <div style="text-align: center; padding: 60px 20px;">
        <div class="hadith-loading-spinner" style="width: 44px; height: 44px; border: 3.5px solid rgba(197,160,89,0.3); border-top-color: var(--border-gold); border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 16px;"></div>
        <h4 style="color: var(--text-dark); margin-bottom: 6px;">جاري فتح ${book.title}...</h4>
        <p style="color: var(--text-muted); font-size: 13px;">تحميل الأحاديث والتحقيقات الشرعية</p>
      </div>
    `;
  }

  try {
    const res = await fetch(`data/hadiths/book_${bookId}.json`);
    if (!res.ok) throw new Error(`HTTP error ${res.status}`);
    hadithState.currentBookData = await res.json();

    applyBookFilters();
    renderHadithFeed();

    // If target hadith was requested (e.g. from global search or jump)
    if (targetHadithNum) {
      setTimeout(() => {
        jumpToHadithNumber(targetHadithNum, false);
      }, 100);
    } else {
      // Check saved progress for this book
      let restored = false;
      try {
        const savedRaw = localStorage.getItem(`sb_book_progress_${bookId}`);
        if (savedRaw) {
          const saved = JSON.parse(savedRaw);
          if (saved && saved.hadithNum && saved.hadithNum > 1) {
            restored = true;
            setTimeout(() => {
              jumpToHadithNumber(saved.hadithNum, false);
              showHadithToast(`تمت استعادة موضع وقوفك الأخير: حديث (${saved.hadithNum.toLocaleString('ar-SA')})`, '🔖');
            }, 120);
          }
        }
      } catch (_) {}

      if (!restored && feed) {
        feed.scrollTop = 0;
      }
    }

  } catch (e) {
    console.error('Error loading book hadiths:', e);
    if (feed) {
      feed.innerHTML = `
        <div style="text-align: center; padding: 40px; background: rgba(255,255,255,0.85); border-radius: 16px; border: 1.5px solid var(--border-gold);">
          <div style="font-size: 34px; margin-bottom: 10px;">⚠️</div>
          <h3 style="color: var(--text-dark); margin-bottom: 6px;">تعذر تحميل أحاديث هذا الكتاب</h3>
          <p style="color: var(--text-muted); font-size: 13px;">يرجى التحقق من اتصالك بالملفات المحلية وإعادة المحاولة.</p>
          <button class="btn-hadith-scope active" onclick="openHadithBook(${bookId})" style="margin-top: 12px; padding: 8px 20px;">إعادة المحاولة ↺</button>
        </div>
      `;
    }
  }
}

// 7. Filter Active Book Hadiths
function applyBookFilters() {
  let list = hadithState.currentBookData;

  // Filter by chapter
  if (hadithState.activeChapter && hadithState.activeChapter !== 'all') {
    list = list.filter(h => h.ch === hadithState.activeChapter);
  }

  // Filter by search query (if in book scope)
  if (hadithState.searchQuery.trim() && hadithState.activeScope === 'book') {
    const qNorm = typeof normalizeArabicText === 'function' ? normalizeArabicText(hadithState.searchQuery) : hadithState.searchQuery.trim();
    list = list.filter(h => {
      const cNorm = h.clean || (typeof normalizeArabicText === 'function' ? normalizeArabicText(h.text) : h.text);
      const chNorm = typeof normalizeArabicText === 'function' ? normalizeArabicText(h.ch || '') : (h.ch || '');
      return cNorm.includes(qNorm) || chNorm.includes(qNorm) || String(h.num) === hadithState.searchQuery.trim();
    });
  }

  hadithState.currentFilteredData = list;

  // Filter Alert update
  const alertEl = document.getElementById('hadithActiveFilterAlert');
  const alertText = document.getElementById('hadithActiveFilterText');
  if (alertEl && alertText) {
    const hasFilter = (hadithState.activeChapter !== 'all') || (hadithState.searchQuery.trim().length > 0 && hadithState.activeScope === 'book');
    if (hasFilter) {
      let desc = '';
      if (hadithState.activeChapter !== 'all') desc += `الباب: "${hadithState.activeChapter}" `;
      if (hadithState.searchQuery.trim()) desc += `البحث: "${hadithState.searchQuery}" `;
      alertText.textContent = `تم العثور على ${list.length.toLocaleString('ar-SA')} حديثاً مطابقة لـ (${desc})`;
      alertEl.style.display = 'flex';
    } else {
      alertEl.style.display = 'none';
    }
  }
}

// 8. Render Hadiths Feed
function renderHadithFeed() {
  const feed = document.getElementById('hadithCardsFeed');
  if (!feed) return;
  feed.innerHTML = '';

  const list = hadithState.currentFilteredData;

  if (list.length === 0) {
    feed.innerHTML = `
      <div style="text-align: center; padding: 50px 20px; background: rgba(255,255,255,0.85); border-radius: 20px; border: 1.5px dashed var(--border-gold);">
        <div style="font-size: 40px; margin-bottom: 12px;">📖</div>
        <h3 style="color: var(--text-dark); margin-bottom: 8px;">لم يتم العثور على أحاديث تطابق المعايير</h3>
        <p style="color: var(--text-muted); font-size: 14px; margin-bottom: 16px;">جرّب تعديل كلمة البحث، أو تغيير الباب، أو التبديل إلى "بحث شامل في كل الكتب".</p>
        <button class="btn-hadith-scope active" onclick="hadithState.activeScope='global'; executeHadithSearch();">
          <span>🌐</span>
          <span>بحث شامل في جميع كتب السنة</span>
        </button>
      </div>
    `;
    return;
  }

  // Render first batch up to renderLimit
  const toRender = list.slice(0, hadithState.renderLimit);
  toRender.forEach(item => {
    feed.appendChild(createHadithCard(item));
  });

  // Render "Load More" indicator if remaining
  if (hadithState.renderLimit < list.length) {
    const moreWrap = document.createElement('div');
    moreWrap.className = 'hadith-feed-load-more-wrap';
    moreWrap.id = 'hadithFeedLoadMore';
    const remaining = list.length - hadithState.renderLimit;
    moreWrap.innerHTML = `
      <button class="btn-hadith-load-more">
        <span>عرض المزيد من الأحاديث (${remaining.toLocaleString('ar-SA')} حديث متبقٍ)</span>
        <span>↓</span>
      </button>
    `;
    moreWrap.querySelector('button').addEventListener('click', () => {
      appendMoreHadiths();
    });
    feed.appendChild(moreWrap);
  }
}

// Append next batch of hadiths
function appendMoreHadiths() {
  const list = hadithState.currentFilteredData;
  if (hadithState.renderLimit >= list.length) return;

  const feed = document.getElementById('hadithCardsFeed');
  if (!feed) return;

  const loadMoreBtn = document.getElementById('hadithFeedLoadMore');
  if (loadMoreBtn) loadMoreBtn.remove();

  const prevLimit = hadithState.renderLimit;
  hadithState.renderLimit += 30;
  const nextBatch = list.slice(prevLimit, hadithState.renderLimit);

  nextBatch.forEach(item => {
    feed.appendChild(createHadithCard(item));
  });

  if (hadithState.renderLimit < list.length) {
    const moreWrap = document.createElement('div');
    moreWrap.className = 'hadith-feed-load-more-wrap';
    moreWrap.id = 'hadithFeedLoadMore';
    const remaining = list.length - hadithState.renderLimit;
    moreWrap.innerHTML = `
      <button class="btn-hadith-load-more">
        <span>عرض المزيد من الأحاديث (${remaining.toLocaleString('ar-SA')} حديث متبقٍ)</span>
        <span>↓</span>
      </button>
    `;
    moreWrap.querySelector('button').addEventListener('click', appendMoreHadiths);
    feed.appendChild(moreWrap);
  }
}

// Jump to specific hadith number in current book
function jumpToHadithNumber(hadithNum, isSmooth = false) {
  const list = hadithState.currentFilteredData;
  const targetIndex = list.findIndex(h => h.num === hadithNum);

  if (targetIndex === -1) {
    // If not in current filtered list, maybe it's in another chapter
    const inFull = hadithState.currentBookData.find(h => h.num === hadithNum);
    if (inFull) {
      hadithState.activeChapter = 'all';
      const selectCh = document.getElementById('selectHadithChapter');
      if (selectCh) selectCh.value = 'all';
      applyBookFilters();
      jumpToHadithNumber(hadithNum, isSmooth);
      return;
    }
    showHadithToast(`الحديث رقم (${hadithNum}) غير موجود في هذا الكتاب`, '⚠️');
    return;
  }

  // Ensure renderLimit covers the target index
  if (targetIndex >= hadithState.renderLimit) {
    hadithState.renderLimit = targetIndex + 20;
    renderHadithFeed();
  }

  // Lock progress saving so smooth/instant scroll doesn't overwrite restored hadith
  hadithState.isRestoringProgress = true;

  requestAnimationFrame(() => {
    setTimeout(() => {
      const card = document.getElementById(`hadith-card-${hadithNum}`);
      const feed = document.getElementById('hadithCardsFeed');
      if (card && feed) {
        const cardRect = card.getBoundingClientRect();
        const feedRect = feed.getBoundingClientRect();
        const targetTop = feed.scrollTop + (cardRect.top - feedRect.top) - 16;
        feed.scrollTo({ top: Math.max(0, targetTop), behavior: isSmooth ? 'smooth' : 'auto' });
        card.classList.add('hadith-card-glow-target');
        setTimeout(() => {
          card.classList.remove('hadith-card-glow-target');
        }, 3200);
        showHadithToast(`تم الانتقال للحديث رقم (${hadithNum})`, '🎯');

        if (hadithState.currentBook) {
          try {
            localStorage.setItem(`sb_book_progress_${hadithState.currentBook.id}`, JSON.stringify({
              hadithNum: hadithNum,
              timestamp: Date.now()
            }));
          } catch (_) {}
        }
      }

      setTimeout(() => {
        hadithState.isRestoringProgress = false;
      }, 800);
    }, 50);
  });
}

// 9. Create Single Hadith Jewel Card Element
function createHadithCard(item, isGlobalResult = false, isFavoriteCard = false) {
  const card = document.createElement('div');
  card.className = 'hadith-jewel-card';
  card.id = `hadith-card-${item.num}`;

  const isFav = hadithState.favorites.some(f => (f.id && f.id === item.id) || (f.book_id === item.book_id && f.num === item.num));
  const bookTitle = item.bookTitle || (hadithState.currentBook ? hadithState.currentBook.title : '');

  // Highlight search keywords if active
  let displayText = item.text;
  if (hadithState.searchQuery.trim()) {
    const re = buildHadithHighlightRegex(hadithState.searchQuery);
    if (re) {
      displayText = item.text.replace(re, '<mark class="hadith-search-highlight">$1</mark>');
    }
  }

  const numFmt = item.num ? item.num.toLocaleString('ar-SA') : '١';

  card.innerHTML = `
    <div class="hadith-card-header">
      <div class="hadith-card-badges">
        <span class="hadith-number-badge">حَدِيثٌ رَقْم: ${numFmt}</span>
        ${bookTitle ? `<span class="hadith-book-badge">📖 ${bookTitle}</span>` : ''}
        ${item.ch ? `<span class="hadith-chapter-badge">📑 ${item.ch}</span>` : ''}
      </div>

      <div class="hadith-card-actions">
        <button class="btn-hadith-act btn-fav ${isFav ? 'active' : ''}" title="${isFav ? 'إزالة من المفضلة' : 'إضافة إلى المفضلة'}">
          <span>${isFav ? '★' : '☆'}</span>
          <span>${isFav ? 'المفضلة' : 'حفظ'}</span>
        </button>
        <button class="btn-hadith-act btn-tts" title="استماع لنص الحديث مسموعاً">
          <span>🔊</span>
          <span>استماع</span>
        </button>
        <button class="btn-hadith-act btn-copy" title="نسخ الحديث وتخريجه">
          <span>📋</span>
          <span>نسخ</span>
        </button>
      </div>
    </div>

    <div class="hadith-card-arabic" style="font-size: ${hadithState.fontSize}px;">
      ${displayText}
    </div>

    ${item.en ? `
      <div class="hadith-card-english" style="display: ${hadithState.showEnglish ? 'block' : 'none'};">
        <div class="english-badge">English Translation</div>
        <div class="english-text">${item.en}</div>
      </div>
    ` : ''}

    <div class="hadith-card-footer">
      <div class="hadith-card-reference">
        <span class="ref-icon">۞</span>
        <span>${item.ref || `${bookTitle} • ${item.ch || ''} • حديث رقم ${item.num}`}</span>
      </div>
      ${isFavoriteCard && item.book_id ? `
        <button class="btn-hadith-open-origin" title="فتح الحديث في كتابه الأصلي">
          <span>قراءة في الكتاب الأصلي</span>
          <span>←</span>
        </button>
      ` : ''}
    </div>
  `;

  // Favorite toggle event
  const favBtn = card.querySelector('.btn-fav');
  if (favBtn) {
    favBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      toggleHadithFavorite(item, favBtn);
    });
  }

  // TTS Speech event
  const ttsBtn = card.querySelector('.btn-tts');
  if (ttsBtn) {
    ttsBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      speakHadithText(item.text, ttsBtn);
    });
  }

  // Copy event
  const copyBtn = card.querySelector('.btn-copy');
  if (copyBtn) {
    copyBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      copyHadithToClipboard(item);
    });
  }

  // Open origin if in favorites
  const originBtn = card.querySelector('.btn-hadith-open-origin');
  if (originBtn && item.book_id) {
    originBtn.addEventListener('click', () => {
      openHadithBook(item.book_id, item.num);
    });
  }

  // Record reading progress when clicking on card
  card.addEventListener('click', (e) => {
    if (e.target.closest('.hadith-card-actions') || e.target.closest('.btn-hadith-act') || e.target.closest('.btn-hadith-open-origin')) return;
    if (hadithState.currentBook && item.num) {
      try {
        localStorage.setItem(`sb_book_progress_${hadithState.currentBook.id}`, JSON.stringify({
          hadithNum: item.num,
          timestamp: Date.now()
        }));
      } catch (_) {}
    }
  });

  return card;
}

// Toggle Hadith Favorite
function toggleHadithFavorite(item, btn) {
  const bookId = item.book_id || (hadithState.currentBook ? hadithState.currentBook.id : null);
  const bookTitle = item.bookTitle || (hadithState.currentBook ? hadithState.currentBook.title : '');

  const idx = hadithState.favorites.findIndex(f => (f.id && f.id === item.id) || (f.book_id === bookId && f.num === item.num));

  if (idx > -1) {
    hadithState.favorites.splice(idx, 1);
    if (btn) {
      btn.classList.remove('active');
      btn.innerHTML = `<span>☆</span><span>حفظ</span>`;
    }
    showHadithToast('تمت الإزالة من المفضلة', '☆');
  } else {
    hadithState.favorites.push({
      ...item,
      book_id: bookId,
      bookTitle: bookTitle
    });
    if (btn) {
      btn.classList.add('active');
      btn.innerHTML = `<span>★</span><span>المفضلة</span>`;
    }
    showHadithToast('تمت الإضافة إلى الأحاديث المفضلة', '⭐');
  }

  try {
    localStorage.setItem('sb_hadith_favorites', JSON.stringify(hadithState.favorites));
  } catch (e) {}

  setupHadithCategoryPills();
}

// Speak Hadith via Web Speech API
function speakHadithText(text, btn) {
  if (!('speechSynthesis' in window)) {
    showHadithToast('النطق الصوتي غير مدعوم في متصفحك', 'ℹ️');
    return;
  }

  if (window.speechSynthesis.speaking) {
    window.speechSynthesis.cancel();
    if (btn) btn.innerHTML = `<span>🔊</span><span>استماع</span>`;
    return;
  }

  const cleanText = text.replace(/[\u064B-\u065F\u0670\u0640]/g, ''); // Clear diacritics for clearer TTS
  const utterance = new SpeechSynthesisUtterance(cleanText);
  utterance.lang = 'ar-SA';
  utterance.rate = 0.92;

  // Pick Arabic voice if available
  const voices = window.speechSynthesis.getVoices();
  const arVoice = voices.find(v => v.lang.startsWith('ar'));
  if (arVoice) utterance.voice = arVoice;

  if (btn) btn.innerHTML = `<span>⏹</span><span>إيقاف</span>`;

  utterance.onend = () => {
    if (btn) btn.innerHTML = `<span>🔊</span><span>استماع</span>`;
  };
  utterance.onerror = () => {
    if (btn) btn.innerHTML = `<span>🔊</span><span>استماع</span>`;
  };

  window.speechSynthesis.speak(utterance);
  showHadithToast('جاري قراءة الحديث النبوي الشريف...', '🔊');
}

// Copy Hadith Text with Reference
function copyHadithToClipboard(item) {
  const bookTitle = item.bookTitle || (hadithState.currentBook ? hadithState.currentBook.title : '');
  const formatted = `عن رسول الله ﷺ:\n"${item.text}"\n\n[التخريج]: ${item.ref || `${bookTitle} - ${item.ch || ''} (حديث رقم: ${item.num})`}\nتطبيق سبح بخشوع`;

  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(formatted).then(() => {
      showHadithToast('تم نسخ الحديث الشريف وتخريجه إلى الحافظة بنجاح', '📋');
    }).catch(() => {
      fallbackCopyHadith(formatted);
    });
  } else {
    fallbackCopyHadith(formatted);
  }
}

function fallbackCopyHadith(text) {
  const ta = document.createElement('textarea');
  ta.value = text;
  document.body.appendChild(ta);
  ta.select();
  try {
    document.execCommand('copy');
    showHadithToast('تم نسخ الحديث الشريف إلى الحافظة بنجاح', '📋');
  } catch (e) {
    showHadithToast('تعذر النسخ إلى الحافظة', '⚠️');
  }
  ta.remove();
}

// 10. Execute Search (Single Book or Global)
function executeHadithSearch() {
  const query = hadithState.searchQuery.trim();

  // If search query is empty, reset to current stage
  if (!query) {
    if (hadithState.currentBook && document.getElementById('hadithReadingStage').style.display !== 'none') {
      applyBookFilters();
      renderHadithFeed();
    } else {
      showHadithShelfStage();
      renderHadithShelf();
    }
    return;
  }

  // Branch by Scope:
  if (hadithState.activeScope === 'global') {
    performGlobalHadithSearch(query);
  } else {
    // If scope is 'book', and a book is open:
    if (hadithState.currentBook && hadithState.currentBookData.length) {
      showHadithReadingStage();
      hadithState.renderLimit = 30;
      applyBookFilters();
      renderHadithFeed();
    } else {
      // If on shelf, filter the shelf books or offer global search
      renderHadithShelf();
    }
  }
}

// 11. Global Comprehensive Search Engine (٥٠,٨٨٤ حديثاً)
async function performGlobalHadithSearch(query) {
  showHadithGlobalSearchStage();

  const resultsList = document.getElementById('globalSearchResultsList');
  const countLabel = document.getElementById('globalSearchCountLabel');
  if (!resultsList) return;

  resultsList.innerHTML = `
    <div style="text-align: center; padding: 60px 20px;">
      <div class="hadith-loading-spinner" style="width: 44px; height: 44px; border: 3.5px solid rgba(197,160,89,0.3); border-top-color: var(--border-gold); border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 16px;"></div>
      <h4 style="color: var(--text-dark); margin-bottom: 6px;">جاري البحث الشامل في ٥٠,٨٨٤ حديثاً شريفاً...</h4>
      <p style="color: var(--text-muted); font-size: 13px;">فحص المتون في البخاري، ومسلم، والسنن الأربعة، والمسانيد</p>
    </div>
  `;

  // Ensure index is loaded
  if (!hadithState.globalIndex) {
    try {
      const res = await fetch('data/hadiths/search_index.json');
      if (!res.ok) throw new Error(`HTTP error ${res.status}`);
      hadithState.globalIndex = await res.json();
    } catch (e) {
      console.error('Error fetching global search index:', e);
      resultsList.innerHTML = `
        <div style="text-align: center; padding: 40px; background: rgba(255,255,255,0.85); border-radius: 16px; border: 1.5px solid var(--border-gold);">
          <div style="font-size: 36px; margin-bottom: 10px;">⚠️</div>
          <h3 style="color: var(--text-dark); margin-bottom: 6px;">تعذر تحميل فهرس البحث الشامل</h3>
          <p style="color: var(--text-muted); font-size: 13px;">يرجى التحقق من اتصالك بالملفات المحلية وإعادة المحاولة.</p>
        </div>
      `;
      return;
    }
  }

  const qNorm = typeof normalizeArabicText === 'function' ? normalizeArabicText(query) : query;
  const re = buildHadithHighlightRegex(query);

  const matches = [];
  const maxMatches = 200; // Fast and smooth UX

  for (let i = 0; i < hadithState.globalIndex.length; i++) {
    const entry = hadithState.globalIndex[i];
    const bId = entry[0];
    const hNum = entry[1];
    const ch = entry[2];
    const snippet = entry[3];

    if (snippet.includes(qNorm) || (ch && ch.includes(qNorm))) {
      matches.push({ bId, hNum, ch, snippet });
      if (matches.length >= maxMatches) break;
    }
  }

  if (countLabel) {
    countLabel.textContent = `تم العثور على ${matches.length.toLocaleString('ar-SA')} حديثاً مباركاً ${matches.length >= maxMatches ? '(تم إظهار أول ٢٠٠ نتيجة)' : ''}`;
  }

  if (matches.length === 0) {
    resultsList.innerHTML = `
      <div style="text-align: center; padding: 60px 20px; background: rgba(255,255,255,0.85); border-radius: 20px; border: 1.5px dashed var(--border-gold);">
        <div style="font-size: 44px; margin-bottom: 14px;">🔍</div>
        <h3 style="color: var(--text-dark); margin-bottom: 8px;">لم يتم العثور على نتائج للبحث الشامل عن: "${query}"</h3>
        <p style="color: var(--text-muted); font-size: 14px; max-width: 480px; margin: 0 auto; line-height: 1.7;">
          تأكد من كتابة الكلمة بشكل صحيح، أو جرّب استخدام مرادفات أو كلمات عامة من متن الحديث (مثال: نية، صلاة، بر، استغفار).
        </p>
      </div>
    `;
    return;
  }

  resultsList.innerHTML = '';
  matches.forEach(m => {
    const book = hadithState.books.find(b => b.id === m.bId);
    const bookTitle = book ? book.title : `كتاب الحديث ${m.bId}`;
    const bookIcon = book ? book.icon : '📖';

    // Extract an excerpt focused on the search match
    let excerpt = m.snippet;
    const matchPos = excerpt.indexOf(qNorm);
    if (matchPos > -1) {
      const start = Math.max(0, matchPos - 75);
      const end = Math.min(excerpt.length, matchPos + qNorm.length + 110);
      excerpt = (start > 0 ? '... ' : '') + excerpt.substring(start, end) + (end < excerpt.length ? ' ...' : '');
    } else if (excerpt.length > 210) {
      excerpt = excerpt.substring(0, 200) + ' ...';
    }

    if (re) {
      excerpt = excerpt.replace(re, '<mark class="hadith-highlight">$1</mark>');
    }

    const itemCard = document.createElement('div');
    itemCard.className = 'global-search-result-card';
    itemCard.innerHTML = `
      <div class="global-result-meta">
        <div class="global-result-book-badge">
          <span>${bookIcon}</span>
          <span>${bookTitle}</span>
        </div>
        <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
          <span class="global-result-ch">📑 ${m.ch || 'باب عام'}</span>
          <span class="hadith-number-badge">حديث: ${m.hNum.toLocaleString('ar-SA')}</span>
        </div>
      </div>

      <p class="global-result-snippet">${excerpt}</p>

      <div class="global-result-cta">
        <span>الانتقال إلى الحديث وقراءته كاملاً في ${bookTitle}</span>
        <span>←</span>
      </div>
    `;

    itemCard.addEventListener('click', () => {
      openHadithBook(m.bId, m.hNum);
    });

    resultsList.appendChild(itemCard);
  });
}


// =========================================================================
// 11. Comprehensive Professional Prayer Times Engine (مواقيت الصلاة الفلكية الدقيقة)
// مطابقة تامة لـ AthanActivity و PrayerTimesRepository في أندرويد
// =========================================================================

const PRAYER_CALC_METHODS = [
  { name: 'جامعة أم القرى (مكة المكرمة)', fajr: 18.5, isha: 90 },
  { name: 'رابطة العالم الإسلامي', fajr: 18.0, isha: 17.0 },
  { name: 'الهيئة العامة المصرية للمساحة', fajr: 19.5, isha: 17.5 },
  { name: 'جامعة العلوم الإسلامية بكراتشي', fajr: 18.0, isha: 18.0 },
  { name: 'الجمعية الإسلامية لأمريكا الشمالية (ISNA)', fajr: 15.0, isha: 15.0 },
  { name: 'دبي (الإمارات العربية المتحدة)', fajr: 18.2, isha: 18.2 },
  { name: 'وزارة الأوقاف القطرية', fajr: 18.0, isha: 90 },
  { name: 'وزارة الأوقاف الكويتية', fajr: 18.0, isha: 17.5 },
  { name: 'سنغافورة (MUIS)', fajr: 20.0, isha: 18.0 },
  { name: 'رئاسة الشؤون الدينية التركية (Diyanet)', fajr: 18.0, isha: 17.0 },
  { name: 'معهد الجيوفيزياء بجامعة طهران', fajr: 17.7, isha: 14.0 }
];

const PRESET_ISLAMIC_CITIES = [
  { name: 'مكة المكرمة', country: 'السعودية', lat: 21.4225, lng: 39.8262, method: 0 },
  { name: 'المدينة المنورة', country: 'السعودية', lat: 24.4672, lng: 39.6111, method: 0 },
  { name: 'الرياض', country: 'السعودية', lat: 24.7136, lng: 46.6753, method: 0 },
  { name: 'جدة', country: 'السعودية', lat: 21.5433, lng: 39.1728, method: 0 },
  { name: 'الدمام', country: 'السعودية', lat: 26.4207, lng: 50.0888, method: 0 },
  { name: 'القاهرة', country: 'مصر', lat: 30.0444, lng: 31.2357, method: 2 },
  { name: 'الإسكندرية', country: 'مصر', lat: 31.2001, lng: 29.9187, method: 2 },
  { name: 'القدس الشريف', country: 'فلسطين', lat: 31.7683, lng: 35.2137, method: 1 },
  { name: 'دبي', country: 'الإمارات', lat: 25.2048, lng: 55.2708, method: 5 },
  { name: 'أبوظبي', country: 'الإمارات', lat: 24.4539, lng: 54.3773, method: 5 },
  { name: 'الدوحة', country: 'قطر', lat: 25.2854, lng: 51.5310, method: 6 },
  { name: 'الكويت', country: 'الكويت', lat: 29.3759, lng: 47.9774, method: 7 },
  { name: 'المنامة', country: 'البحرين', lat: 26.2285, lng: 50.5860, method: 0 },
  { name: 'مسقط', country: 'عُمان', lat: 23.5880, lng: 58.3829, method: 0 },
  { name: 'عَمّان', country: 'الأردن', lat: 31.9539, lng: 35.9106, method: 1 },
  { name: 'دمشق', country: 'سوريا', lat: 33.5138, lng: 36.2765, method: 1 },
  { name: 'بيروت', country: 'لبنان', lat: 33.8938, lng: 35.5018, method: 1 },
  { name: 'بغداد', country: 'العراق', lat: 33.3152, lng: 44.3661, method: 1 },
  { name: 'صنعاء', country: 'اليمن', lat: 15.3694, lng: 44.1910, method: 0 },
  { name: 'طرابلس', country: 'ليبيا', lat: 32.8872, lng: 13.1913, method: 1 },
  { name: 'تونس', country: 'تونس', lat: 36.8065, lng: 10.1815, method: 1 },
  { name: 'الجزائر', country: 'الجزائر', lat: 36.7538, lng: 3.0588, method: 1 },
  { name: 'الرباط', country: 'المغرب', lat: 34.0209, lng: -6.8416, method: 1 },
  { name: 'الدار البيضاء', country: 'المغرب', lat: 33.5731, lng: -7.5898, method: 1 },
  { name: 'الخرطوم', country: 'السودان', lat: 15.5007, lng: 32.5599, method: 2 },
  { name: 'إسطنبول', country: 'تركيا', lat: 41.0082, lng: 28.9784, method: 9 },
  { name: 'أنقرة', country: 'تركيا', lat: 39.9334, lng: 32.8597, method: 9 },
  { name: 'كراتشي', country: 'باكستان', lat: 24.8607, lng: 67.0011, method: 3 },
  { name: 'لاهور', country: 'باكستان', lat: 31.5204, lng: 74.3587, method: 3 },
  { name: 'جاكرتا', country: 'إندونيسيا', lat: -6.2088, lng: 106.8456, method: 1 },
  { name: 'كوالالمبور', country: 'ماليزيا', lat: 3.1390, lng: 101.6869, method: 1 },
  { name: 'لندن', country: 'بريطانيا', lat: 51.5074, lng: -0.1278, method: 1 },
  { name: 'باريس', country: 'فرنسا', lat: 48.8566, lng: 2.3522, method: 1 },
  { name: 'نيويورك', country: 'أمريكا', lat: 40.7128, lng: -74.0060, method: 4 }
];

const CLOUD_ATHAN_SOUNDS = [
  { id: 'default', title: 'صوت الأذان الافتراضي (المدمج)', desc: 'أذان ندي مدمج بالتطبيق يعمل دون إنترنت', url: '../assets/audio/athan.mp3' },
  { id: 'athan_1', title: 'صوت الأذان الأول (Athan 1)', desc: 'أذان خاشع كامل ومؤثر', url: 'https://hywewsfxlxouxjpbkmgr.supabase.co/storage/v1/object/public/athan-audio/athan1.mp3' },
  { id: 'athan_090_3', title: 'صوت الأذان الثاني (090-3)', desc: 'أذان بصوت ندي وواضح', url: 'https://hywewsfxlxouxjpbkmgr.supabase.co/storage/v1/object/public/athan-audio/090-3.mp3' },
  { id: 'athan_101_3', title: 'صوت الأذان الثالث (101-3)', desc: 'أذان مميز ونقي', url: 'https://hywewsfxlxouxjpbkmgr.supabase.co/storage/v1/object/public/athan-audio/101-3.mp3' },
  { id: 'athan_106_4', title: 'صوت الأذان الرابع (106-4)', desc: 'أذان حجازي رفيع', url: 'https://hywewsfxlxouxjpbkmgr.supabase.co/storage/v1/object/public/athan-audio/106-4.mp3' },
  { id: 'athan_147', title: 'صوت الأذان الخامس (147)', desc: 'أذان سريع وخفيف', url: 'https://hywewsfxlxouxjpbkmgr.supabase.co/storage/v1/object/public/athan-audio/147.mp3' },
  { id: 'athan_152_4', title: 'صوت الأذان السادس (152-4)', desc: 'أذان خاشع ومؤثر', url: 'https://hywewsfxlxouxjpbkmgr.supabase.co/storage/v1/object/public/athan-audio/152-4.mp3' },
  { id: 'athan_168_4', title: 'صوت الأذان السابع (168-4)', desc: 'أذان بمقام هادئ ووقور', url: 'https://hywewsfxlxouxjpbkmgr.supabase.co/storage/v1/object/public/athan-audio/168-4.mp3' },
  { id: 'athan_makkah', title: 'أذان الحرم المكي الشريف', desc: 'أذان المسجد الحرام بمكة المكرمة', url: 'https://www.islamcan.com/audio/adhan/makkah.mp3' },
  { id: 'athan_madinah', title: 'أذان الحرم النبوي المدني', desc: 'أذان المسجد النبوي الشريف بالمدينة المنورة', url: 'https://www.islamcan.com/audio/adhan/madina.mp3' },
  { id: 'athan_aqsa', title: 'أذان المسجد الأقصى المبارك', desc: 'أذان رحاب المسجد الأقصى بالقدس الشريف', url: 'https://www.islamcan.com/audio/adhan/aqsa.mp3' },
  { id: 'athan_alafasy', title: 'أذان الشيخ مشاري راشد العفاسي', desc: 'أذان ندي بصوت الشيخ مشاري العفاسي', url: 'https://download.quranicaudio.com/athan/mishary_rashid_alafasy.mp3' },
  { id: 'athan_abdulbasit', title: 'أذان الشيخ عبد الباسط عبد الصمد', desc: 'أذان تاريخي مهيب بصوت الشيخ عبد الباسط', url: 'https://download.quranicaudio.com/athan/abdul_basit.mp3' }
];

function setupPrayerEngine() {
  // --- A. Local Preferences & State ---
  let lat = parseFloat(localStorage.getItem('sb_prayer_lat') || '21.4225');
  let lng = parseFloat(localStorage.getItem('sb_prayer_lng') || '39.8262');
  let cityName = localStorage.getItem('sb_prayer_city') || 'مكة المكرمة - المملكة العربية السعودية';
  let calcMethodIdx = parseInt(localStorage.getItem('sb_prayer_calc_method') || '0', 10);
  let madhab = localStorage.getItem('sb_prayer_madhab') || 'shafi';
  let isGlobalSoundEnabled = (localStorage.getItem('sb_prayer_global_sound') !== 'false');
  let alertType = localStorage.getItem('sb_prayer_alert_type') || 'full'; // 'full', 'takbeerat', 'gentle', 'silent'
  let bannerStyle = localStorage.getItem('sb_prayer_banner_style') || 'both'; // 'both', 'modal', 'system'
  let earlyReminderMins = parseInt(localStorage.getItem('sb_prayer_early_reminder') || '0', 10);
  let iqamaReminderMins = parseInt(localStorage.getItem('sb_prayer_iqama_reminder') || '0', 10);
  let activeSoundId = localStorage.getItem('sb_prayer_sound_id') || 'default';
  let customAudioUrl = localStorage.getItem('sb_prayer_custom_audio_url') || '';

  // Per-prayer sound toggle & offsets
  let prayerSoundToggles = { FAJR: true, SUNRISE: false, DHUHR: true, ASR: true, MAGHRIB: true, ISHA: true };
  try {
    const savedToggles = localStorage.getItem('sb_prayer_sound_toggles');
    if (savedToggles) prayerSoundToggles = { ...prayerSoundToggles, ...JSON.parse(savedToggles) };
  } catch (_) {}

  let prayerOffsets = { FAJR: 0, SUNRISE: 0, DHUHR: 0, ASR: 0, MAGHRIB: 0, ISHA: 0 };
  try {
    const savedOffsets = localStorage.getItem('sb_prayer_offsets');
    if (savedOffsets) prayerOffsets = { ...prayerOffsets, ...JSON.parse(savedOffsets) };
  } catch (_) {}

  // Audio elements
  let activeAudio = null;
  let previewAudio = null;
  let takbeeratTimeout = null;
  let lastNotifiedKey = null;
  let lastPreNotifiedKey = null;

  // --- B. Astronomical Calculation Functions ---
  function deg2rad(d) { return d * Math.PI / 180.0; }
  function rad2deg(r) { return r * 180.0 / Math.PI; }
  function fixHour(h) { return h - 24.0 * Math.floor(h / 24.0); }
  function dsin(d) { return Math.sin(deg2rad(d)); }
  function dcos(d) { return Math.cos(deg2rad(d)); }
  function darccos(x) { return rad2deg(Math.acos(Math.max(-1, Math.min(1, x)))); }
  function darccot(x) { return rad2deg(Math.atan(1.0 / x)); }

  function computePrayerTimesForDate(date) {
    let year = date.getFullYear();
    let month = date.getMonth() + 1;
    let day = date.getDate();
    let tz = -date.getTimezoneOffset() / 60.0;

    let y = year, m = month;
    if (m <= 2) { y -= 1; m += 12; }
    let A = Math.floor(y / 100);
    let B = 2 - A + Math.floor(A / 4);
    let jd = Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + day + B - 1524.5;
    let D = jd - 2451545.0;
    let g = (357.529 + 0.98560028 * D) % 360;
    let q = (280.459 + 0.98564736 * D) % 360;
    let L = (q + 1.915 * dsin(g) + 0.020 * dsin(2 * g)) % 360;
    let e = 23.439 - 0.00000036 * D;
    let dec = rad2deg(Math.asin(dsin(e) * dsin(L)));
    let RA = rad2deg(Math.atan2(dcos(e) * dsin(L), dcos(L))) / 15;
    RA = fixHour(RA);
    let EqT = q / 15 - RA;

    let noon = fixHour(12 + tz - lng / 15 - EqT);

    function sunHourAngle(altitude, morning) {
      let cosT = (dsin(altitude) - dsin(lat) * dsin(dec)) / (dcos(lat) * dcos(dec));
      if (cosT > 1 || cosT < -1) return null;
      let T = darccos(cosT) / 15;
      return fixHour(noon + (morning ? -T : T));
    }

    function asrHour(factor) {
      let diff = Math.abs(lat - dec);
      let alt = darccot(factor + Math.tan(deg2rad(diff)));
      return sunHourAngle(alt, false);
    }

    const curMethod = PRAYER_CALC_METHODS[calcMethodIdx] || PRAYER_CALC_METHODS[0];
    const fajrAngle = curMethod.fajr;
    const ishaRule = curMethod.isha;
    const asrShadow = (madhab === 'hanafi') ? 2 : 1;

    let fajrH = sunHourAngle(-fajrAngle, true);
    let sunriseH = sunHourAngle(-0.833, true);
    let dhuhrH = noon;
    let asrH = asrHour(asrShadow);
    let maghribH = sunHourAngle(-0.833, false);
    let ishaH = (typeof ishaRule === 'number' && ishaRule < 30) ? sunHourAngle(-ishaRule, false) : fixHour(maghribH + ishaRule / 60);

    function toDateObj(hVal, offsetMin) {
      if (hVal === null || isNaN(hVal)) return new Date();
      let totalMins = Math.round(hVal * 60) + (offsetMin || 0);
      let res = new Date(date);
      res.setHours(0, 0, 0, 0);
      res.setMinutes(totalMins);
      return res;
    }

    return {
      FAJR: toDateObj(fajrH, prayerOffsets.FAJR),
      SUNRISE: toDateObj(sunriseH, prayerOffsets.SUNRISE),
      DHUHR: toDateObj(dhuhrH, prayerOffsets.DHUHR),
      ASR: toDateObj(asrH, prayerOffsets.ASR),
      MAGHRIB: toDateObj(maghribH, prayerOffsets.MAGHRIB),
      ISHA: toDateObj(ishaH, prayerOffsets.ISHA)
    };
  }

  function formatTimeArabic12(dateObj) {
    if (!dateObj) return '--:--';
    let h = dateObj.getHours();
    let m = dateObj.getMinutes();
    let p = h >= 12 ? 'م' : 'ص';
    let h12 = h % 12 || 12;
    return `${String(h12).padStart(2, '0')}:${String(m).padStart(2, '0')} ${p}`;
  }

  function getPrayerArabicName(key) {
    switch (key) {
      case 'FAJR': return 'صلاة الفجر';
      case 'SUNRISE': return 'الشروق';
      case 'DHUHR': return 'صلاة الظهر';
      case 'ASR': return 'صلاة العصر';
      case 'MAGHRIB': return 'صلاة المغرب';
      case 'ISHA': return 'صلاة العشاء';
      default: return 'الصلاة';
    }
  }

  function getPrayerIcon(key) {
    switch (key) {
      case 'FAJR': return '🌅';
      case 'SUNRISE': return '☀️';
      case 'DHUHR': return '🌤️';
      case 'ASR': return '⛅';
      case 'MAGHRIB': return '🌇';
      case 'ISHA': return '🌙';
      default: return '🕌';
    }
  }

  // --- C. Audio & Sound Controller ---
  function getActiveSoundObject() {
    if (activeSoundId === 'custom' && customAudioUrl) {
      return { id: 'custom', title: 'صوت مخصص من الجهاز', url: customAudioUrl };
    }
    const found = CLOUD_ATHAN_SOUNDS.find(s => s.id === activeSoundId);
    return found || CLOUD_ATHAN_SOUNDS[0];
  }

  function playGentleBellTone() {
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();
      const now = ctx.currentTime;

      [523.25, 659.25, 783.99, 1046.50].forEach((freq, idx) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, now + idx * 0.22);
        gain.gain.setValueAtTime(0.3, now + idx * 0.22);
        gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.22 + 1.2);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(now + idx * 0.22);
        osc.stop(now + idx * 0.22 + 1.25);
      });
    } catch (_) {}
  }

  function stopAllAthanAudio() {
    if (activeAudio) {
      try { activeAudio.pause(); } catch (_) {}
      activeAudio = null;
    }
    if (previewAudio) {
      try { previewAudio.pause(); } catch (_) {}
      previewAudio = null;
    }
    if (takbeeratTimeout) {
      clearTimeout(takbeeratTimeout);
      takbeeratTimeout = null;
    }

    const btnStop = document.getElementById('btnStopAthan');
    if (btnStop) btnStop.style.display = 'none';
    const btnStopModal = document.getElementById('btnStopAthanPreviewModal');
    if (btnStopModal) btnStopModal.style.display = 'none';

    const modalAlarm = document.getElementById('modalAthanAlarm');
    if (modalAlarm) modalAlarm.classList.remove('open');
  }

  function triggerAthanSound(isTest = false) {
    stopAllAthanAudio();

    if (!isGlobalSoundEnabled && !isTest) return;
    if (alertType === 'silent' && !isTest) return;

    if (alertType === 'gentle') {
      playGentleBellTone();
      return;
    }

    const soundObj = getActiveSoundObject();
    activeAudio = new Audio(soundObj.url);

    activeAudio.play().then(() => {
      const btnStop = document.getElementById('btnStopAthan');
      if (btnStop) btnStop.style.display = 'inline-flex';

      // If takbeerat only: stop after 22 seconds
      if (alertType === 'takbeerat') {
        takbeeratTimeout = setTimeout(() => {
          stopAllAthanAudio();
        }, 22000);
      }
    }).catch(e => {
      console.warn('Athan audio play error:', e);
    });

    activeAudio.onended = () => {
      stopAllAthanAudio();
    };
  }

  // --- D. Notification & Alarm Modal Trigger ---
  function showAthanNotification(prayerKey, prayerName, isTest = false) {
    // 1. Play Audio
    triggerAthanSound(isTest);

    // 2. Fullscreen / Modal Banner
    if (bannerStyle === 'modal' || bannerStyle === 'both' || isTest) {
      const modalAlarm = document.getElementById('modalAthanAlarm');
      const pNameEl = document.getElementById('alarmPrayerNameDisplay');
      const pInfoEl = document.getElementById('alarmTimeInfoDisplay');
      const pSoundTitle = document.getElementById('alarmSoundPlayingTitle');

      if (pNameEl) pNameEl.textContent = prayerName;
      if (pInfoEl) pInfoEl.textContent = `حان الآن موعد أذان ${prayerName} بحسب التوقيت المحلي لمدينة ${cityName}`;
      if (pSoundTitle) {
        const soundObj = getActiveSoundObject();
        pSoundTitle.textContent = (alertType === 'takbeerat') 
          ? 'تكبيرات الأذان المبارك...' 
          : (alertType === 'gentle' ? 'نغمة التنبيه الهادئ...' : `رفع الأذان: ${soundObj.title}`);
      }
      if (modalAlarm) modalAlarm.classList.add('open');
    }

    // 3. Desktop Windows Notification
    if (bannerStyle === 'system' || bannerStyle === 'both' || isTest) {
      try {
        if ('Notification' in window) {
          if (Notification.permission === 'granted') {
            new Notification(`حان الآن موعد ${prayerName}`, {
              body: `حان الآن موعد أذان ${prayerName} بتوقيت ${cityName}.\nحي على الصلاة.. حي على الفلاح`,
              icon: '../assets/icons/icon.png',
              silent: true // Audio is handled via our audio player
            });
          } else if (Notification.permission !== 'denied') {
            Notification.requestPermission();
          }
        }
      } catch (_) {}
    }
  }

  // --- E. UI Rendering & Tick Loop ---
  function renderPrayerTable(times, nextKey) {
    const container = document.getElementById('prayerTableRowsContainer');
    if (!container) return;

    const keys = ['FAJR', 'SUNRISE', 'DHUHR', 'ASR', 'MAGHRIB', 'ISHA'];
    container.innerHTML = '';

    keys.forEach(k => {
      const isNext = (k === nextKey);
      const isSoundOn = prayerSoundToggles[k] !== false;
      const offsetVal = prayerOffsets[k] || 0;
      const formattedTime = formatTimeArabic12(times[k]);

      const row = document.createElement('div');
      row.className = `prayer-row-item ${isNext ? 'prayer-row-next-highlight' : ''}`;
      row.id = `prayer-row-${k}`;

      row.innerHTML = `
        <div class="prayer-row-name-col">
          <span class="prayer-row-icon">${getPrayerIcon(k)}</span>
          <span class="prayer-row-name">${getPrayerArabicName(k)}</span>
        </div>

        <div class="prayer-offset-controls">
          <button class="btn-offset-adjust" data-action="dec" data-prayer="${k}" title="تأخير دقيقة">-</button>
          <span class="prayer-offset-val">${offsetVal > 0 ? '+' + offsetVal : offsetVal}</span>
          <button class="btn-offset-adjust" data-action="inc" data-prayer="${k}" title="تقديم دقيقة">+</button>
        </div>

        <div class="prayer-time-col">
          <span class="prayer-time-val">${formattedTime}</span>
          <button class="btn-prayer-sound-toggle" data-prayer="${k}" title="${isSoundOn ? 'كتم صوت هذه الصلاة' : 'تفعيل صوت هذه الصلاة'}">
            ${isSoundOn ? '🔊' : '🔇'}
          </button>
        </div>
      `;

      // Event listeners for +/- offset
      row.querySelectorAll('.btn-offset-adjust').forEach(btn => {
        btn.addEventListener('click', (e) => {
          e.stopPropagation();
          const action = btn.getAttribute('data-action');
          const p = btn.getAttribute('data-prayer');
          prayerOffsets[p] = (prayerOffsets[p] || 0) + (action === 'inc' ? 1 : -1);
          localStorage.setItem('sb_prayer_offsets', JSON.stringify(prayerOffsets));
          updateAllPrayerTimes();
        });
      });

      // Event listener for sound toggle icon
      const soundBtn = row.querySelector('.btn-prayer-sound-toggle');
      if (soundBtn) {
        soundBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          prayerSoundToggles[k] = !isSoundOn;
          localStorage.setItem('sb_prayer_sound_toggles', JSON.stringify(prayerSoundToggles));
          updateAllPrayerTimes();
        });
      }

      container.appendChild(row);
    });
  }

  function updateDatesBadge(now) {
    const gregEl = document.getElementById('prayerGregorianDate');
    const hijriEl = document.getElementById('prayerHijriDate');

    if (gregEl) {
      try {
        const opt = { day: 'numeric', month: 'long', year: 'numeric' };
        gregEl.textContent = now.toLocaleDateString('ar-EG', opt);
      } catch (_) {
        gregEl.textContent = `${now.getDate()}/${now.getMonth() + 1}/${now.getFullYear()}`;
      }
    }

    if (hijriEl) {
      try {
        const opt = { day: 'numeric', month: 'long', year: 'numeric' };
        hijriEl.textContent = now.toLocaleDateString('ar-SA-u-ca-islamic-umalqura', opt);
      } catch (_) {
        hijriEl.textContent = '١٤٤٨ هـ';
      }
    }
  }

  function updateLocationLabels() {
    const cityEl = document.getElementById('prayerCityText');
    const coordsEl = document.getElementById('prayerCoordsText');
    const detailedCoordsEl = document.getElementById('prayerDetailedCoordsNote');

    if (cityEl) cityEl.textContent = cityName;
    if (coordsEl) coordsEl.textContent = `الإحداثيات: ${lat.toFixed(4)}, ${lng.toFixed(4)}`;
    if (detailedCoordsEl) detailedCoordsEl.textContent = `الإحداثيات الحالية: ${lat.toFixed(4)}° N, ${lng.toFixed(4)}° E`;
  }

  function updateAllPrayerTimes() {
    const now = new Date();
    updateDatesBadge(now);
    updateLocationLabels();

    const times = computePrayerTimesForDate(now);

    // Determine next prayer
    const keys = ['FAJR', 'SUNRISE', 'DHUHR', 'ASR', 'MAGHRIB', 'ISHA'];
    let nextKey = null;
    let nextDate = null;

    for (let k of keys) {
      if (times[k] > now) {
        nextKey = k;
        nextDate = times[k];
        break;
      }
    }

    if (!nextKey) {
      // Tomorrow's Fajr
      const tomorrow = new Date(now);
      tomorrow.setDate(now.getDate() + 1);
      const tomorrowTimes = computePrayerTimesForDate(tomorrow);
      nextKey = 'FAJR';
      nextDate = tomorrowTimes.FAJR;
    }

    // Update Header Card
    const titleEl = document.getElementById('prayerNextTitle');
    const countdownEl = document.getElementById('prayerCountdownDisplay');

    if (titleEl) {
      titleEl.textContent = `الصلاة القادمة ${getPrayerArabicName(nextKey)}`;
    }

    // Update Home Interactive Bead
    const expPrayerName = document.getElementById('homeExpNextPrayerName');
    const expPrayerTime = document.getElementById('homeExpNextPrayerTime');
    const expPrayerRemaining = document.getElementById('homeExpNextPrayerRemaining');

    if (expPrayerName) {
      expPrayerName.textContent = `صلاة ${getPrayerArabicName(nextKey)}`;
    }

    if (expPrayerTime && nextDate) {
      try {
        expPrayerTime.textContent = nextDate.toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' });
      } catch (_) {
        const hh = nextDate.getHours();
        const mm = String(nextDate.getMinutes()).padStart(2, '0');
        expPrayerTime.textContent = `${hh % 12 || 12}:${mm} ${hh >= 12 ? 'م' : 'ص'}`;
      }
    }

    if (countdownEl && nextDate) {
      let diffSec = Math.max(0, Math.floor((nextDate - now) / 1000));
      let dh = Math.floor(diffSec / 3600);
      let dm = Math.floor((diffSec % 3600) / 60);
      let ds = diffSec % 60;
      const formattedDiff = `${String(dh).padStart(2, '0')}:${String(dm).padStart(2, '0')}:${String(ds).padStart(2, '0')}`;
      countdownEl.textContent = formattedDiff;

      if (expPrayerRemaining) {
        expPrayerRemaining.textContent = `متبقي ${formattedDiff}`;
      }

      // Update Card Prayer Subtitle in Standard Cards Mode
      const cardPrayerSub = document.getElementById('tvCardPrayerSubtitle');
      if (cardPrayerSub) {
        cardPrayerSub.textContent = `صلاة ${getPrayerArabicName(nextKey)} متبقي عليها ${formattedDiff}`;
      }

      // Check Pre-Prayer Early Alert
      if (earlyReminderMins > 0 && diffSec === earlyReminderMins * 60) {
        if (lastPreNotifiedKey !== nextKey) {
          lastPreNotifiedKey = nextKey;
          try {
            if ('Notification' in window && Notification.permission === 'granted') {
              new Notification(`اقترب موعد ${getPrayerArabicName(nextKey)}`, {
                body: `متبقي ${earlyReminderMins} دقائق على أذان ${getPrayerArabicName(nextKey)} بتوقيت ${cityName}.`,
                icon: '../assets/icons/icon.png'
              });
            }
          } catch (_) {}
        }
      }

      // Check Exact Prayer Time Alert
      if (diffSec === 0) {
        if (lastNotifiedKey !== nextKey) {
          lastNotifiedKey = nextKey;
          if (prayerSoundToggles[nextKey] !== false) {
            showAthanNotification(nextKey, getPrayerArabicName(nextKey), false);
          }
        }
      }
    }

    // Render Table Rows
    renderPrayerTable(times, nextKey);
  }

  // Live second-by-second clock
  function liveClockTick() {
    const now = new Date();
    const clockEl = document.getElementById('prayerLiveClock');
    if (clockEl) {
      clockEl.textContent = now.toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    }
    updateAllPrayerTimes();
  }

  // --- F. Location Management (Auto & Manual) ---
  function populatePresetCities() {
    const grid = document.getElementById('prayerPresetCitiesGrid');
    const searchInput = document.getElementById('inputCitySearch');
    if (!grid) return;

    function render(filter = '') {
      grid.innerHTML = '';
      const norm = filter.trim().toLowerCase();
      const filtered = PRESET_ISLAMIC_CITIES.filter(c => c.name.toLowerCase().includes(norm) || c.country.toLowerCase().includes(norm));

      filtered.forEach(c => {
        const btn = document.createElement('button');
        btn.className = `btn-city-chip ${cityName.includes(c.name) ? 'active' : ''}`;
        btn.textContent = `${c.name} (${c.country})`;
        btn.addEventListener('click', () => {
          lat = c.lat;
          lng = c.lng;
          cityName = `${c.name} - ${c.country}`;
          calcMethodIdx = c.method;

          localStorage.setItem('sb_prayer_lat', lat);
          localStorage.setItem('sb_prayer_lng', lng);
          localStorage.setItem('sb_prayer_city', cityName);
          localStorage.setItem('sb_prayer_calc_method', calcMethodIdx);

          const selectCalc = document.getElementById('selectCalcMethod');
          if (selectCalc) selectCalc.value = calcMethodIdx;

          const modal = document.getElementById('modalManualLocation');
          if (modal) modal.classList.remove('open');

          updateAllPrayerTimes();
        });
        grid.appendChild(btn);
      });
    }

    render();
    if (searchInput) {
      searchInput.addEventListener('input', () => render(searchInput.value));
    }
  }

  async function detectLocationAuto(isManualClick = true) {
    if (btnAutoLoc) btnAutoLoc.textContent = 'جاري التحديد... 🧭';

    async function fallbackIpLocation() {
      try {
        const res = await fetch('https://ipwho.is/').catch(() => null);
        if (res && res.ok) {
          const data = await res.json();
          if (data && data.latitude && data.longitude) {
            lat = parseFloat(data.latitude);
            lng = parseFloat(data.longitude);
            const city = data.city || '';
            const country = data.country || '';
            cityName = (city && country) ? `${city} - ${country}` : (city || country || 'الموقع الجغرافي');

            if (country.includes('Saudi') || country.includes('السعودية')) calcMethodIdx = 0;
            else if (country.includes('Egypt') || country.includes('مصر')) calcMethodIdx = 2;
            else if (country.includes('Emirates') || country.includes('الإمارات')) calcMethodIdx = 5;
            else if (country.includes('Qatar') || country.includes('قطر')) calcMethodIdx = 6;
            else if (country.includes('Kuwait') || country.includes('الكويت')) calcMethodIdx = 7;
            else if (country.includes('Turkey') || country.includes('تركيا')) calcMethodIdx = 9;
            else if (country.includes('Pakistan') || country.includes('باكستان')) calcMethodIdx = 3;
            else if (country.includes('United States') || country.includes('Canada')) calcMethodIdx = 4;
            else calcMethodIdx = 1;

            localStorage.setItem('sb_prayer_lat', lat);
            localStorage.setItem('sb_prayer_lng', lng);
            localStorage.setItem('sb_prayer_city', cityName);
            localStorage.setItem('sb_prayer_calc_method', calcMethodIdx);

            if (btnAutoLoc) btnAutoLoc.textContent = '🧭 تحديد تلقائي (GPS / الشبكة)';
            updateAllPrayerTimes();
            if (isManualClick) {
              alert(`تم تحديد موقعك بدقة عبر الشبكة بنجاح:\n📍 ${cityName}\nخط العرض: ${lat.toFixed(4)}\nخط الطول: ${lng.toFixed(4)}`);
            }
            return true;
          }
        }

        const res2 = await fetch('https://freeipapi.com/api/json').catch(() => null);
        if (res2 && res2.ok) {
          const data2 = await res2.json();
          if (data2 && data2.latitude && data2.longitude) {
            lat = parseFloat(data2.latitude);
            lng = parseFloat(data2.longitude);
            const city = data2.cityName || '';
            const country = data2.countryName || '';
            cityName = (city && country) ? `${city} - ${country}` : 'الموقع الجغرافي';
            localStorage.setItem('sb_prayer_lat', lat);
            localStorage.setItem('sb_prayer_lng', lng);
            localStorage.setItem('sb_prayer_city', cityName);
            if (btnAutoLoc) btnAutoLoc.textContent = '🧭 تحديد تلقائي (GPS / الشبكة)';
            updateAllPrayerTimes();
            if (isManualClick) {
              alert(`تم تحديد موقعك بدقة:\n📍 ${cityName}\nخط العرض: ${lat.toFixed(4)}\nخط الطول: ${lng.toFixed(4)}`);
            }
            return true;
          }
        }
      } catch (e) {
        console.warn('IP location fallback error:', e);
      }
      return false;
    }

    if (navigator.geolocation) {
      let resolved = false;
      const timeoutTimer = setTimeout(async () => {
        if (!resolved) {
          resolved = true;
          await fallbackIpLocation();
        }
      }, 3500);

      navigator.geolocation.getCurrentPosition(
        (pos) => {
          if (resolved) return;
          resolved = true;
          clearTimeout(timeoutTimer);
          lat = pos.coords.latitude;
          lng = pos.coords.longitude;
          cityName = 'الموقع الجغرافي المباشر (GPS)';
          localStorage.setItem('sb_prayer_lat', lat);
          localStorage.setItem('sb_prayer_lng', lng);
          localStorage.setItem('sb_prayer_city', cityName);

          if (btnAutoLoc) btnAutoLoc.textContent = '🧭 تحديد تلقائي (GPS / الشبكة)';
          updateAllPrayerTimes();
          if (isManualClick) {
            alert(`تم تحديد موقعك بنجاح بنظام GPS:\nخط العرض: ${lat.toFixed(4)}\nخط الطول: ${lng.toFixed(4)}`);
          }
        },
        async (err) => {
          if (resolved) return;
          resolved = true;
          clearTimeout(timeoutTimer);
          const ok = await fallbackIpLocation();
          if (!ok && isManualClick) {
            if (btnAutoLoc) btnAutoLoc.textContent = '🧭 تحديد تلقائي (GPS / الشبكة)';
            alert('تعذر تحديد الموقع تلقائياً. يمكنك اختيار مدينتك من قائمة المدن بنقرة واحدة.');
          }
        },
        { enableHighAccuracy: true, timeout: 3000 }
      );
    } else {
      await fallbackIpLocation();
    }
  }

  const btnAutoLoc = document.getElementById('btnAutoLocation');
  if (btnAutoLoc) {
    btnAutoLoc.addEventListener('click', () => detectLocationAuto(true));
  }

  const btnManualLoc = document.getElementById('btnManualLocation');
  const modalLoc = document.getElementById('modalManualLocation');
  const btnCloseLoc = document.getElementById('btnCloseLocationModal');
  const btnSaveCustomLoc = document.getElementById('btnSaveCustomLocation');

  if (btnManualLoc && modalLoc) {
    btnManualLoc.addEventListener('click', () => {
      populatePresetCities();
      modalLoc.classList.add('open');
    });
  }
  if (btnCloseLoc && modalLoc) {
    btnCloseLoc.addEventListener('click', () => modalLoc.classList.remove('open'));
  }
  if (modalLoc) {
    modalLoc.addEventListener('click', (e) => {
      if (e.target === modalLoc) modalLoc.classList.remove('open');
    });
  }

  if (btnSaveCustomLoc) {
    btnSaveCustomLoc.addEventListener('click', () => {
      const latVal = parseFloat(document.getElementById('inputCustomLat')?.value);
      const lngVal = parseFloat(document.getElementById('inputCustomLng')?.value);
      const cityVal = document.getElementById('inputCustomCityName')?.value.trim();

      if (isNaN(latVal) || isNaN(lngVal)) {
        alert('يرجى إدخال خط طول وعرض صحيحين.');
        return;
      }
      lat = latVal;
      lng = lngVal;
      cityName = cityVal || `موقع مخصص (${lat.toFixed(2)}, ${lng.toFixed(2)})`;

      localStorage.setItem('sb_prayer_lat', lat);
      localStorage.setItem('sb_prayer_lng', lng);
      localStorage.setItem('sb_prayer_city', cityName);

      if (modalLoc) modalLoc.classList.remove('open');
      updateAllPrayerTimes();
    });
  }

  // --- G. Sound Chooser & Muezzin Modal ---
  function updateSoundLabels() {
    const soundObj = getActiveSoundObject();
    const subTitleEl = document.getElementById('prayerActiveSoundSubTitle');
    const pathEl = document.getElementById('tvSelectedSoundPath');
    if (subTitleEl) subTitleEl.textContent = soundObj.title;
    if (pathEl) pathEl.textContent = `الصوت المحدد: ${soundObj.title}`;
  }

  function renderAthanSoundsList() {
    const container = document.getElementById('athanSoundsListContainer');
    if (!container) return;
    container.innerHTML = '';

    CLOUD_ATHAN_SOUNDS.forEach(s => {
      const isCurrent = (activeSoundId === s.id);
      const card = document.createElement('div');
      card.className = `athan-sound-card ${isCurrent ? 'active-sound' : ''}`;

      card.innerHTML = `
        <div class="athan-sound-card-titles">
          <span class="athan-sound-card-name">${s.title}</span>
          <span class="athan-sound-card-desc">${s.desc}</span>
        </div>
        <div class="athan-sound-card-btns">
          <button class="btn-sound-preview" data-url="${s.url}">▶ استماع</button>
          <button class="btn-sound-adopt" data-id="${s.id}">${isCurrent ? '✓ المعتمد' : 'اعتماد'}</button>
        </div>
      `;

      // Preview Button
      const previewBtn = card.querySelector('.btn-sound-preview');
      previewBtn.addEventListener('click', () => {
        if (previewAudio) {
          previewAudio.pause();
          previewAudio = null;
        }
        previewAudio = new Audio(s.url);
        previewBtn.textContent = 'جاري التشغيل...';
        const stopModalBtn = document.getElementById('btnStopAthanPreviewModal');
        if (stopModalBtn) stopModalBtn.style.display = 'block';

        previewAudio.play().catch(e => console.warn(e));
        previewAudio.onended = () => {
          previewBtn.textContent = '▶ استماع';
          if (stopModalBtn) stopModalBtn.style.display = 'none';
        };
      });

      // Adopt Button
      const adoptBtn = card.querySelector('.btn-sound-adopt');
      adoptBtn.addEventListener('click', () => {
        activeSoundId = s.id;
        localStorage.setItem('sb_prayer_sound_id', activeSoundId);
        updateSoundLabels();
        renderAthanSoundsList();
      });

      container.appendChild(card);
    });
  }

  const btnOpenSoundModal = document.getElementById('btnChooseSoundDialog');
  const modalSound = document.getElementById('modalChooseAthanSound');
  const btnCloseSound = document.getElementById('btnCloseAthanSoundModal');
  const btnStopPreview = document.getElementById('btnStopAthanPreviewModal');
  const inputLocalAudio = document.getElementById('inputLocalAudioFile');

  if (btnOpenSoundModal && modalSound) {
    btnOpenSoundModal.addEventListener('click', () => {
      renderAthanSoundsList();
      modalSound.classList.add('open');
    });
  }
  if (btnCloseSound && modalSound) {
    btnCloseSound.addEventListener('click', () => {
      if (previewAudio) previewAudio.pause();
      modalSound.classList.remove('open');
    });
  }
  if (modalSound) {
    modalSound.addEventListener('click', (e) => {
      if (e.target === modalSound) {
        if (previewAudio) previewAudio.pause();
        modalSound.classList.remove('open');
      }
    });
  }
  if (btnStopPreview) {
    btnStopPreview.addEventListener('click', () => {
      if (previewAudio) {
        previewAudio.pause();
        previewAudio = null;
      }
      btnStopPreview.style.display = 'none';
      renderAthanSoundsList();
    });
  }

  if (inputLocalAudio) {
    inputLocalAudio.addEventListener('change', (e) => {
      const file = e.target.files[0];
      if (file) {
        const objUrl = URL.createObjectURL(file);
        activeSoundId = 'custom';
        customAudioUrl = objUrl;
        localStorage.setItem('sb_prayer_sound_id', 'custom');
        localStorage.setItem('sb_prayer_custom_audio_url', customAudioUrl);
        updateSoundLabels();
        if (modalSound) modalSound.classList.remove('open');
        alert(`تم اعتماد الملف الصوتي بنجاح: ${file.name}`);
      }
    });
  }

  // --- H. Settings Accordion & Form Controls ---
  const headerSettings = document.getElementById('layoutPrayerSettingsHeader');
  const bodySettings = document.getElementById('layoutPrayerSettingsBody');
  const indSettings = document.getElementById('prayerDropdownIndicator');

  if (headerSettings && bodySettings) {
    headerSettings.addEventListener('click', () => {
      const isClosed = (bodySettings.style.display === 'none');
      bodySettings.style.display = isClosed ? 'flex' : 'none';
      if (indSettings) indSettings.textContent = isClosed ? '▲' : '▼';
    });
  }

  // Calculation Method Dropdown
  const selectCalc = document.getElementById('selectCalcMethod');
  if (selectCalc) {
    selectCalc.value = calcMethodIdx;
    selectCalc.addEventListener('change', () => {
      calcMethodIdx = parseInt(selectCalc.value, 10);
      localStorage.setItem('sb_prayer_calc_method', calcMethodIdx);
      updateAllPrayerTimes();
    });
  }

  // Madhab Dropdown
  const selectM = document.getElementById('selectMadhab');
  if (selectM) {
    selectM.value = madhab;
    selectM.addEventListener('change', () => {
      madhab = selectM.value;
      localStorage.setItem('sb_prayer_madhab', madhab);
      updateAllPrayerTimes();
    });
  }

  // Global Sound Switch
  const switchGlobal = document.getElementById('switchGlobalSound');
  const globalIcon = document.getElementById('prayerGlobalSoundIcon');
  const globalTitle = document.getElementById('prayerGlobalSoundTitle');

  function updateGlobalSoundUI() {
    if (switchGlobal) switchGlobal.checked = isGlobalSoundEnabled;
    if (globalIcon) globalIcon.textContent = isGlobalSoundEnabled ? '🔊' : '🔇';
    if (globalTitle) globalTitle.textContent = isGlobalSoundEnabled ? 'صوت الأذان مفعل' : 'صوت الأذان مكتوم';
  }
  updateGlobalSoundUI();

  if (switchGlobal) {
    switchGlobal.addEventListener('change', () => {
      isGlobalSoundEnabled = switchGlobal.checked;
      localStorage.setItem('sb_prayer_global_sound', isGlobalSoundEnabled ? 'true' : 'false');
      updateGlobalSoundUI();
    });
  }

  // Alert Type Buttons
  const alertTypeBtns = document.querySelectorAll('#prayerAlertTypeButtons .btn-alert-type');
  alertTypeBtns.forEach(b => {
    if (b.getAttribute('data-type') === alertType) {
      alertTypeBtns.forEach(x => x.classList.remove('active'));
      b.classList.add('active');
    }
    b.addEventListener('click', () => {
      alertTypeBtns.forEach(x => x.classList.remove('active'));
      b.classList.add('active');
      alertType = b.getAttribute('data-type');
      localStorage.setItem('sb_prayer_alert_type', alertType);
    });
  });

  // Banner Style Selector
  const selectBanner = document.getElementById('selectBannerStyle');
  if (selectBanner) {
    selectBanner.value = bannerStyle;
    selectBanner.addEventListener('change', () => {
      bannerStyle = selectBanner.value;
      localStorage.setItem('sb_prayer_banner_style', bannerStyle);
    });
  }

  // Early Reminder Selector
  const selectEarly = document.getElementById('selectEarlyReminder');
  if (selectEarly) {
    selectEarly.value = earlyReminderMins;
    selectEarly.addEventListener('change', () => {
      earlyReminderMins = parseInt(selectEarly.value, 10);
      localStorage.setItem('sb_prayer_early_reminder', earlyReminderMins);
    });
  }

  // Iqama Reminder Selector
  const selectIqama = document.getElementById('selectIqamaReminder');
  if (selectIqama) {
    selectIqama.value = iqamaReminderMins;
    selectIqama.addEventListener('change', () => {
      iqamaReminderMins = parseInt(selectIqama.value, 10);
      localStorage.setItem('sb_prayer_iqama_reminder', iqamaReminderMins);
    });
  }

  // Test Athan Button & Stop Button
  const btnTest = document.getElementById('btnTestAthan');
  const btnStop = document.getElementById('btnStopAthan');
  if (btnTest) {
    btnTest.addEventListener('click', () => {
      showAthanNotification('DHUHR', 'صلاة الظهر', true);
    });
  }
  if (btnStop) {
    btnStop.addEventListener('click', () => {
      stopAllAthanAudio();
    });
  }

  // Modal Alarm Buttons
  const btnDismissAlarm = document.getElementById('btnDismissAthanAlarm');
  const btnSnoozeAlarm = document.getElementById('btnSnoozeAthanAlarm');
  if (btnDismissAlarm) {
    btnDismissAlarm.addEventListener('click', () => {
      stopAllAthanAudio();
    });
  }
  if (btnSnoozeAlarm) {
    btnSnoozeAlarm.addEventListener('click', () => {
      stopAllAthanAudio();
      setTimeout(() => {
        showAthanNotification('DHUHR', 'تذكير بوقت الصلاة', false);
      }, 10 * 60 * 1000);
      alert('تم تفعيل التذكير بالصلاة بعد 10 دقائق ⏰');
    });
  }

  // Initial Run & Live Tick Timer
  updateSoundLabels();
  liveClockTick();
  setInterval(liveClockTick, 1000);

  // Auto-detect location on first launch if not yet saved
  if (!localStorage.getItem('sb_prayer_city')) {
    detectLocationAuto(false);
  }
}

// --- Global Toast Notification Helper ---
function showAppToast(message, type = 'info', duration = 4500) {
  let container = document.getElementById('appToastContainer');
  if (!container) {
    container = document.createElement('div');
    container.id = 'appToastContainer';
    container.className = 'app-toast-container';
    document.body.appendChild(container);
  }
  const toast = document.createElement('div');
  toast.className = `app-toast toast-${type}`;
  toast.innerHTML = `<span>${message}</span>`;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(15px)';
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

// --- 12. Islamic Sunnah AI Assistant & Gemini Web Integration Module ---
const SUNNAH_SYSTEM_PROMPT = `أنت مساعد شرعي وإسلامي متخصص في القرآن الكريم وتفسيره والحديث النبوي الشريف والعلوم الشرعية وتدبر الآيات.
يجب أن تكون جميع إجاباتك وشروحاتك واستدلالاتك حصرًا ومقيدة تماماً بما يلي:
1. كتاب الله عز وجل وسنة رسوله ﷺ الصحيحة.
2. معتقد ومنهج ومذهب أهل السنة والجماعة بفهم السلف الصالح رضوان الله عليهم.
3. الاعتماد على الأحاديث الصحيحة من أمهات كتب السنة (صحيح البخاري، صحيح مسلم، والسنن المعتمدة).
4. الاعتماد في التفسير على أئمة التفسير بالمأثور المعتمدين عند أهل السنة (كابن كثير، الطبري، السعدي، والقرطبي).
5. تجنب الآراء الشاذة أو الأقوال المبتدعة أو كل ما يخالف عقيدة ومنهج أهل السنة والجماعة.
6. تقديم الإجابة بلطف وأدب وتوثيق للأدلة وتيسير على السائل، والبدء بالسلام الشرعي.`;

function buildSunnahGeminiWebPrompt(userQuestion) {
  return `بصفتك باحثاً في العلوم الشرعية الإسلامية، أجب عن السؤال التالي حصرًا ومقيداً بكتاب الله وسنة رسوله ﷺ وعلى معتقد ومنهج ومذهب أهل السنة والجماعة بفهم السلف الصالح، بالاعتماد على الأحاديث الصحيحة وتفاسير أئمة أهل السنة المعتمدين (ابن كثير، الطبري، السعدي، القرطبي)، مع تجنب الآراء الشاذة أو المخالفة:\n\nالسؤال:\n${userQuestion}`;
}

function openInGeminiWeb(question) {
  const q = (question && question.trim()) || 'ما هي قواعد تدبر القرآن الكريم بفهم السلف الصالح؟';
  const targetPrompt = buildSunnahGeminiWebPrompt(q);
  
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(targetPrompt).catch(() => {});
  }
  
  const geminiUrl = 'https://gemini.google.com/app';
  if (window.electronAPI && window.electronAPI.openExternal) {
    window.electronAPI.openExternal(geminiUrl);
  } else {
    window.open(geminiUrl, '_blank');
  }
  
  showAppToast('✅ تم نسخ السؤال مؤطراً بمنهج أهل السنة والجماعة! جارٍ فتح متصفح Gemini... الصق السؤال (Ctrl+V) هناك.', 'success', 6000);
}

// Local Authentic Ahlus-Sunnah Knowledge Base for Instant & Offline Answers
const SUNNAH_LOCAL_KB = [
  {
    keywords: ['الكرسي', 'آية الكرسي'],
    title: 'تدبر آية الكرسي ومعاني التوحيد وأسماء الله الحسنى',
    response: `وعليكم السلام ورحمة الله وبركاته.

<strong>تدبر آية الكرسي [سورة البقرة: 255] وفق تفسير أئمة أهل السنة (ابن كثير والسعدي):</strong>

آية الكرسي هي <strong>أعظم آية في كتاب الله تعالى</strong> بنص حديث رسول الله ﷺ لأبي بن كعب رضي الله عنه (رواه مسلم).

<strong>أهم المعاني والفوائد العقدية والإيمانية:</strong>
1. <strong>﴿اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ﴾:</strong> إثبات توحيد الألوهية وإفراده سبحانه بالعبادة، فلا معبود بحق إلا الله وحده.
2. <strong>﴿الْحَيُّ الْقَيُّومُ﴾:</strong> اسمان عظيمان عليهما مدار الأسماء الحسنى؛ فالحي متضمن لجميع صفات الذات كالعلم والقدرة والسمع والبصر، والقيوم متضمن لجميع صفات الأفعال، فهو القائم بنفسه والمقيم لكل ما سواه.
3. <strong>﴿لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ﴾:</strong> كمال الحياة والقيومية، ونفي العجز والنعاس والغفلة، لأن النوم أخو الموت ومنزه عنه سبحانه.
4. <strong>﴿لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ﴾:</strong> إثبات توحيد الربوبية والملك التام الشامل؛ فالخلق كلهم عبيده ومماليكه.
5. <strong>﴿مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ﴾:</strong> بيان عظمته وجلاله، وأن الشفاعة لا تملك استقلالاً كما يزعم أهل الشرك، بل لا تكون إلا بشرطين: إذن الله للشافع، ورضاه عن المشفوع له.
6. <strong>﴿يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ﴾:</strong> شمول علمه الأزلي بالماضي والحاضر والمستقبل، وقصور علم الخلائق إلا ما علمهم إياه.
7. <strong>﴿وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ﴾:</strong> الكرسي موضع القدمين للرب سبحانه كما صح عن ابن عباس، وهو مخلوق عظيم السماوات السبع والأرض فيه كحلقة ملقاة في فلاة.
8. <strong>﴿وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ﴾:</strong> لا يثقله ولا يكترثه حفظ الكون كله، وهو العلي بذاته وقدره وقهره، العظيم في ملكه وسلطانه.

<strong>فضلها:</strong> قراءتها دبر كل صلاة مكتوبة سبب لدخول الجنة (رواه النسائي وصححه الألباني)، وقراءتها عند النوم حرز من الشيطان.`
  },
  {
    keywords: ['الاستغفار', 'سيد الاستغفار', 'صيغ الاستغفار'],
    title: 'أعظم صيغ الاستغفار وفضائله في السنة النبوية الصحيحة',
    response: `وعليكم السلام ورحمة الله وبركاته.

<strong>صيغ الاستغفار وثمراته في ضوء الكتاب وصحيح السنة:</strong>

الاستغفار هو طلب المغفرة والستر ومحو الذنب والوقاية من شؤمه.

<strong>أولاً: أعظم صيغ الاستغفار (سيد الاستغفار):</strong>
روى البخاري في صحيحه عن شداد بن أوس رضي الله عنه عن النبي ﷺ قال: «سَيِّدُ الاِسْتِغْفَارِ أَنْ تَقُولَ: اللَّهُمَّ أَنْتَ رَبِّي لاَ إِلَهَ إِلاَّ أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ لَكَ بِذَنْبِي، فَاغْفِرْ لِي فَإِنَّهُ لاَ يَغْفِرُ الذُّنُوبَ إِلاَّ أَنْتَ».
<strong>فضله:</strong> من قاله موقناً به حين يمسي فمات دخل الجنة، ومن قاله موقناً به حين يصبح فمات دخل الجنة.

<strong>ثانياً: صيغ مأثورة صحيحة أخرى:</strong>
1. «أَسْتَغْفِرُ اللَّهَ الَّذِي لاَ إِلَهَ إِلاَّ هُوَ الْحَيَّ الْقَيُّومَ وَأَتُوبُ إِلَيْهِ» (غُفرت ذنوبه وإن كان فر من الزحف - رواه أبو داود والترمذي).
2. «رَبِّ اغْفِرْ لِي وَتُبْ عَلَيَّ إِنَّكَ أَنْتَ التَّوَّابُ الرَّحِيمُ» (كان النبي ﷺ يعد له في المجلس الواحد مائة مرة - رواه أبو داود وصححه الألباني).
3. «أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ» (مائة مرة يومياً).

<strong>ثالثاً: ثمار الاستغفار في القرآن:</strong>
مغفرة الذنوب، نزول الغيث المدرار، البركة في الأموال والبنين، وتفريج الكروب والهموم كما قال تعالى حكاية عن نوح عليه السلام: ﴿فَقُلْتُ اسْتَغْفِرُوا رَبَّكُمْ إِنَّهُ كَانَ غَفَّارًا * يُرْسِلِ السَّمَاءَ عَلَيْكُم مِّدْرَارًا * وَيُمْدِدْكُم بِأَمْوَالٍ وَبَنِينَ وَيَجْعَل لَّكُمْ جَنَّاتٍ وَيَجْعَل لَّكُمْ أَنْهَارًا﴾.`
  },
  {
    keywords: ['النيات', 'الأعمال بالنيات', 'إنما الأعمال'],
    title: 'شرح حديث «إنما الأعمال بالنيات» ومنزلته عند أهل السنة',
    response: `وعليكم السلام ورحمة الله وبركاته.

<strong>شرح حديث «إنما الأعمال بالنيات» وفق منهج أهل السنة والجماعة:</strong>

عن أمير المؤمنين عمر بن الخطاب رضي الله عنه قال: سمعت رسول الله ﷺ يقول:
«إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى، فَمَنْ كَانَتْ هِجْرَتُهُ إِلَى دُنْيَا يُصِيبُهَا أَوْ إِلَى امْرَأَةٍ يَنْكِحُهَا، فَهِجْرَتُهُ إِلَى مَا هَاجَرَ إِلَيْهِ» [رواه البخاري ومسلم].

<strong>مكانة الحديث:</strong>
قال الإمام الشافعي والإمام أحمد: "هذا الحديث يدخل في سبعين باباً من الفقه، وهو ثلث الإسلام".

<strong>المعاني والفوائد المستنبطة:</strong>
1. <strong>شرط قبول العمل:</strong> لا يصح عمل ولا يقبل عند الله إلا بشرطين متلازمين:
   - <strong>الإخلاص لله وحده:</strong> وهو مقتضى حديث النية ومقتضى شهادة "أن لا إله إلا الله".
   - <strong>المتابعة لرسول الله ﷺ:</strong> وهو مقتضى شهادة "أن محمداً رسول الله".
2. <strong>وظيفة النية:</strong> تمييز العبادة من العادة (كالغسل للتبرد أو للجنابة)، وتمييز رتب العبادات بعضها من بعض (كصلاة الفرض من النفل).
3. <strong>التحذير من الرياء والشرك الأصغر:</strong> فالعمل الذي يراد به غير وجه الله حابط ومردود على صاحبه.
4. <strong>تحويل المباحات إلى طاعات:</strong> بنية التقوي على طاعة الله (كالأكل والنوم بنية التنشط للعبادة والعمل الحلال).`
  },
  {
    keywords: ['سجود التلاوة', 'أحكام سجود'],
    title: 'أحكام وشروط سجود التلاوة والدعاء المشروع فيه',
    response: `وعليكم السلام ورحمة الله وبركاته.

<strong>أحكام سجود التلاوة وفق المعتمد عند محققي أهل السنة:</strong>

<strong>1. حكمه:</strong> سنة مؤكدة للقارئ والمستمع، وليس بواجب على الراجح؛ لحديث عمر بن الخطاب رضي الله عنه لما قرأ سورة النحل على المنبر فنزل وسجد، ثم قرأها في الجمعة التالية فلم يسجد وقال: «يا أيها الناس إنا نمر بالسجود فمن سجد فقد أصاب ومن لم يسجد فلا إثم عليه» [رواه البخاري].

<strong>2. شروطه:</strong>
- الراجح أنه لا تشترط له الطهارة الصغرى ولا استقبال القبلة ولا ستر العورة شرط صحة كالصلاة الكاملة لأنه سجدة مجردة وخضوع (كما اختاره ابن تيمية وابن القيم وابن باز وابن عثيمين)، وإن كانت الطهارة والاستقبال أفضل وأكمل بلا خلاف.

<strong>3. كيفيته:</strong>
- يكبر إذا سجد في الصلاة رفعاً وخفضاً. أما خارج الصلاة فيكبر إذا سجد ولا يشترط تكبير للرفع ولا تسليم على الراجح.

<strong>4. الدعاء المشروع فيه:</strong>
- يسبح: «سبحان ربي الأعلى» (ثلاثاً).
- ويدعو بما ثبت عن النبي ﷺ:
  «سَجَدَ وَجْهِي لِلَّذِي خَلَقَهُ، وَشَقَّ سَمْعَهُ وَبَصَرَهُ، بِحَوْلِهِ وَقُوَّتِهِ، فَتَبَارَكَ اللَّهُ أَحْسَنُ الْخَالِقِينَ» [رواه الترمذي وصححه].
  «اللَّهُمَّ اكْتُبْ لِي بِهَا عِنْدَكَ أَجْرًا، وَضَعْ عَنِّي بِهَا وِزْرًا، وَاجْعَلْهَا لِي عِنْدَكَ ذُخْرًا، وَتَقَبَّلْهَا مِنِّي كَمَا تَقَبَّلْتَهَا مِنْ عَبْدِكَ دَاوُدَ» [رواه الترمذي وحسنه].`
  },
  {
    keywords: ['أدعية جامعة', 'دعاء جامع', 'أدعية مأثورة'],
    title: 'أدعية جامعة مأثورة من القرآن وصحيح السنة',
    response: `وعليكم السلام ورحمة الله وبركاته.

<strong>من أجمع الأدعية المأثورة في الكتاب والسنة النبوية الصحيحة:</strong>

<strong>أولاً: من جوامع دعاء القرآن الكريم:</strong>
1. ﴿رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ﴾ [البقرة: 201] (كان أكثر دعاء النبي ﷺ كما روى البخاري ومسلم).
2. ﴿رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِن لَّدُنكَ رَحْمَةً ۚ إِنَّكَ أَنتَ الْوَهَّابُ﴾ [آل عمران: 8].
3. ﴿رَّبِّ اغْفِرْ وَارْحَمْ وَأَنتَ خَيْرُ الرَّاحِمِينَ﴾ [المؤمنون: 118].

<strong>ثانياً: من جوامع الأدعية النبوية الصحيحة:</strong>
1. «اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَى وَالتُّقَى وَالْعَفَافَ وَالْغِنَى» [رواه مسلم].
2. «اللَّهُمَّ أَصْلِحْ لِي دِينِي الَّذِي هُوَ عِصْمَةُ أَمْرِي، وَأَصْلِحْ لِي دُنْيَايَ الَّتِي فِيهَا مَعَاشِي، وَأَصْلِحْ لِي آخِرَتِي الَّتِي فِيهَا مَعَادِي، وَاجْعَلِ الْحَيَاةَ زِيَادَةً لِي فِي كُلِّ خَيْرٍ، وَاجْعَلِ الْمَوْتَ رَاحَةً لِي مِنْ كُلِّ شَرٍّ» [رواه مسلم].
3. «يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَى دِينِكَ» [رواه الترمذي وصححه الألباني].
4. «اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ، وَضَلَعِ الدَّيْنِ، وَغَلَبَةِ الرِّجَالِ» [رواه البخاري].`
  },
  {
    keywords: ['السلف', 'تدبر القرآن', 'فهم السلف'],
    title: 'قواعد تدبر القرآن الكريم بفهم السلف الصالح',
    response: `وعليكم السلام ورحمة الله وبركاته.

<strong>قواعد تدبر القرآن الكريم بفهم السلف الصالح رضوان الله عليهم:</strong>

قال شيخ الإسلام ابن تيمية رحمه الله: "فإن أصح الطرق في ذلك أن يفسر القرآن بالقرآن، فما أُجمل في مكان فإنه قد فُسر في موضع آخر، فإن أعياك ذلك فعليك بالسنة، فإنها شارحة للقرآن وموضحة له... وحينئذ إذا لم نجد التفسير في القرآن ولا في السنة رجعنا في ذلك إلى أقوال الصحابة، فإنهم أدرى بذلك لما شاهدوه من القرآن والقرائن التي اختصوا بها... وإذا لم تجد التفسير في القرآن ولا في السنة ولا وجدته عن الصحابة فقد رجع كثير من الأئمة إلى أقوال التابعين".

<strong>أبرز قواعد التدبر السلفي الصحيح:</strong>
1. <strong>تفسير القرآن بالقرآن:</strong> فالآيات يصدق بعضها بعضاً ويفسر بعضها بعضاً.
2. <strong>تفسير القرآن بالسنة النبوية الصحيحة:</strong> فالسنة وحي مبين للقرآن، قال تعالى: ﴿وَأَنزَلْنَا إِلَيْكَ الذِّكْرَ لِتُبَيِّنَ لِلنَّاسِ مَا نُزِّلَ إِلَيْهِمْ﴾.
3. <strong>اعتماد فهم الصحابة والتابعين وأئمة اللغة:</strong> فهم أعمق الناس علماً وأبرهم قلوباً وأعلمهم بلسان العرب ومقاصد الشريعة.
4. <strong>الاستعانة بكتب التفسير بالمأثور:</strong> مثل: (جامع البيان للطبري، وتفسير القرآن العظيم لابن كثير، وتيسير الكريم الرحمن للسعدي).
5. <strong>الجمع بين العلم والعمل:</strong> كما قال أبو عبد الرحمن السلمي: "حدثنا الذين كانوا يقرئوننا القرآن كعثمان بن عفان وعبد الله بن مسعود وغيرهما أنهم كانوا إذا تعلموا من النبي ﷺ عشر آيات لم يجاوزوها حتى يتعلموا ما فيها من العلم والعمل، قالوا: فتعلمنا القرآن والعلم والعمل جميعاً".
6. <strong>تجنب القول بالرأي المجرد أو التأويلات الباطلة:</strong> التي تصرف الألفاظ عن حقائقها بلا برهان من لغة العرب أو الشرع.`
  }
];

function findSunnahLocalAnswer(query) {
  if (!query) return null;
  const qClean = query.toLowerCase().trim();
  for (const item of SUNNAH_LOCAL_KB) {
    for (const kw of item.keywords) {
      if (qClean.includes(kw.toLowerCase())) {
        return item.response;
      }
    }
  }
  return null;
}

function renderMessageContentHtml(rawText) {
  if (!rawText) return '';
  // Format Quranic quotes with distinctive style
  let formatted = rawText
    .replace(/﴿([^﴾]+)﴾/g, '<span class="quran-quote">﴿$1﴾</span>')
    .replace(/"([^"]+)"\s*\[سورة/g, '<span class="quran-quote">«$1»</span> [سورة');
  
  return formatted;
}

// Master Setup for Assistant
function setupIslamicAIAssistant() {
  const chatStream = document.getElementById('assistantChatHistory');
  const inputEl = document.getElementById('assistantInput');
  const sendBtn = document.getElementById('btnSendAssistant');
  const btnSendToGeminiDirect = document.getElementById('btnSendToGeminiDirect');
  const btnOpenGeminiWebTop = document.getElementById('btnOpenGeminiWebTop');
  const btnBannerOpenGemini = document.getElementById('btnBannerOpenGemini');
  const btnOpenApiKeyModal = document.getElementById('btnOpenApiKeyModal');
  const btnClearAssistantChat = document.getElementById('btnClearAssistantChat');
  const typingIndicator = document.getElementById('assistantTypingIndicator');

  // API Modal elements
  const modalApiKey = document.getElementById('modalAssistantApiKey');
  const btnCloseApiKeyModal = document.getElementById('btnCloseApiKeyModal');
  const inputGeminiApiKey = document.getElementById('inputGeminiApiKey');
  const selectGeminiModel = document.getElementById('selectGeminiModel');
  const btnSaveApiKey = document.getElementById('btnSaveApiKey');
  const btnClearApiKey = document.getElementById('btnClearApiKey');
  const btnTestApiKey = document.getElementById('btnTestApiKey');
  const btnToggleApiKeyVisibility = document.getElementById('btnToggleApiKeyVisibility');
  const apiKeyTestStatus = document.getElementById('apiKeyTestStatus');

  // Load saved API settings
  const savedKey = localStorage.getItem('sb_gemini_api_key') || '';
  const savedModel = localStorage.getItem('sb_gemini_model') || 'gemini-2.0-flash';
  if (inputGeminiApiKey) inputGeminiApiKey.value = savedKey;
  if (selectGeminiModel) selectGeminiModel.value = savedModel;

  // Auto-resize textarea
  if (inputEl) {
    inputEl.addEventListener('input', () => {
      inputEl.style.height = 'auto';
      inputEl.style.height = Math.min(inputEl.scrollHeight, 120) + 'px';
    });
    inputEl.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
      }
    });
  }

  // Gemini Web Button Handlers
  function triggerGeminiWebWithInput() {
    const q = (inputEl && inputEl.value.trim()) || '';
    openInGeminiWeb(q);
  }

  if (btnOpenGeminiWebTop) btnOpenGeminiWebTop.addEventListener('click', triggerGeminiWebWithInput);
  if (btnBannerOpenGemini) btnBannerOpenGemini.addEventListener('click', triggerGeminiWebWithInput);
  if (btnSendToGeminiDirect) btnSendToGeminiDirect.addEventListener('click', triggerGeminiWebWithInput);

  // Suggested Topics Chips
  document.querySelectorAll('.assistant-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      const prompt = chip.getAttribute('data-prompt');
      if (prompt && inputEl) {
        inputEl.value = prompt;
        inputEl.style.height = 'auto';
        inputEl.style.height = Math.min(inputEl.scrollHeight, 120) + 'px';
        sendMessage();
      }
    });
  });

  // Clear Chat History
  if (btnClearAssistantChat) {
    btnClearAssistantChat.addEventListener('click', () => {
      if (!chatStream) return;
      chatStream.innerHTML = `
        <div class="chat-msg-row bot-row">
          <div class="chat-avatar bot-avatar">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2 2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zm-9 6a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4v10a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V8zm7 4.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3zm4 0a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3zM9 16h6a1 1 0 0 0 0-2H9a1 1 0 0 0 0 2z"/></svg>
          </div>
          <div class="chat-bubble bot-bubble">
            <div class="bubble-header">
              <span class="sender-name">المساعد الشرعي للتدبر</span>
              <span class="sunnah-tag">أهل السنة والجماعة</span>
            </div>
            <div class="bubble-content">
              السلام عليكم ورحمة الله وبركاته.<br>
              تم بدء جلسة جديدة. تفضل بطرح أي سؤال أو تدبر قرآني وفق منهج أهل السنة والجماعة.
            </div>
          </div>
        </div>
      `;
      showAppToast('تم مسح سجل المحادثة بنجاح', 'info');
    });
  }

  // API Key Modal Toggles
  if (btnOpenApiKeyModal && modalApiKey) {
    btnOpenApiKeyModal.addEventListener('click', () => {
      modalApiKey.classList.add('open');
      if (apiKeyTestStatus) apiKeyTestStatus.style.display = 'none';
    });
  }
  if (btnCloseApiKeyModal && modalApiKey) {
    btnCloseApiKeyModal.addEventListener('click', () => modalApiKey.classList.remove('open'));
  }
  if (modalApiKey) {
    modalApiKey.addEventListener('click', (e) => {
      if (e.target === modalApiKey) modalApiKey.classList.remove('open');
    });
  }

  if (btnToggleApiKeyVisibility && inputGeminiApiKey) {
    btnToggleApiKeyVisibility.addEventListener('click', () => {
      inputGeminiApiKey.type = inputGeminiApiKey.type === 'password' ? 'text' : 'password';
      btnToggleApiKeyVisibility.textContent = inputGeminiApiKey.type === 'password' ? '👁️' : '🙈';
    });
  }

  if (btnSaveApiKey && inputGeminiApiKey) {
    btnSaveApiKey.addEventListener('click', () => {
      const key = inputGeminiApiKey.value.trim();
      const model = selectGeminiModel ? selectGeminiModel.value : 'gemini-2.0-flash';
      localStorage.setItem('sb_gemini_api_key', key);
      localStorage.setItem('sb_gemini_model', model);
      showAppToast('✅ تم حفظ مفتاح Google Gemini بنجاح على جهازك!', 'success');
      if (modalApiKey) modalApiKey.classList.remove('open');
    });
  }

  if (btnClearApiKey && inputGeminiApiKey) {
    btnClearApiKey.addEventListener('click', () => {
      localStorage.removeItem('sb_gemini_api_key');
      inputGeminiApiKey.value = '';
      if (apiKeyTestStatus) {
        apiKeyTestStatus.style.display = 'block';
        apiKeyTestStatus.style.background = '#FEE2E2';
        apiKeyTestStatus.style.color = '#991B1B';
        apiKeyTestStatus.textContent = 'تم مسح المفتاح من الذاكرة المحلية.';
      }
      showAppToast('تم مسح المفتاح', 'info');
    });
  }

  // Test API Key Connection (Health Check)
  if (btnTestApiKey && inputGeminiApiKey) {
    btnTestApiKey.addEventListener('click', async () => {
      const key = inputGeminiApiKey.value.trim();
      const model = selectGeminiModel ? selectGeminiModel.value : 'gemini-2.0-flash';
      if (!key) {
        if (apiKeyTestStatus) {
          apiKeyTestStatus.style.display = 'block';
          apiKeyTestStatus.style.background = '#FEF3C7';
          apiKeyTestStatus.style.color = '#92400E';
          apiKeyTestStatus.textContent = 'يرجى إدخال المفتاح أولاً لتجربته.';
        }
        return;
      }

      if (apiKeyTestStatus) {
        apiKeyTestStatus.style.display = 'block';
        apiKeyTestStatus.style.background = '#EFF6FF';
        apiKeyTestStatus.style.color = '#1E40AF';
        apiKeyTestStatus.textContent = '⏳ جاري اختبار الاتصال بخوادم Google Gemini...';
      }

      try {
        const resp = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${key}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            contents: [{ parts: [{ text: 'مرحبا، هل الخدمة متصلة؟ أجب بكلمة واحدة: نعم' }] }]
          })
        });

        if (resp.ok) {
          const data = await resp.json();
          const reply = data.candidates?.[0]?.content?.parts?.[0]?.text || '';
          if (apiKeyTestStatus) {
            apiKeyTestStatus.style.background = '#DCFCE7';
            apiKeyTestStatus.style.color = '#166534';
            apiKeyTestStatus.innerHTML = `✅ <strong>تم الاتصال بنجاح!</strong> رد النموذج (${model}): "${reply.trim()}". المفتاح صالح وجاهز للاستخدام.`;
          }
          showAppToast('✅ تم التحقق من المفتاح بنجاح!', 'success');
        } else {
          const errData = await resp.json().catch(() => ({}));
          const errMsg = errData.error?.message || `كود الخطأ: ${resp.status}`;
          if (apiKeyTestStatus) {
            apiKeyTestStatus.style.background = '#FEE2E2';
            apiKeyTestStatus.style.color = '#991B1B';
            apiKeyTestStatus.innerHTML = `❌ <strong>تعذر الاتصال:</strong> ${errMsg}<br><small>تأكد من تفعيل Generative Language API وصحة المفتاح.</small>`;
          }
        }
      } catch (err) {
        if (apiKeyTestStatus) {
          apiKeyTestStatus.style.background = '#FEE2E2';
          apiKeyTestStatus.style.color = '#991B1B';
          apiKeyTestStatus.textContent = `❌ خطأ في الاتصال بالإنترنت أو تعذر الوصول: ${err.message}`;
        }
      }
    });
  }

  // Send Message Logic
  async function sendMessage() {
    if (!inputEl) return;
    const text = inputEl.value.trim();
    if (!text) return;
    inputEl.value = '';
    inputEl.style.height = 'auto';

    // 1. Append User Message Bubble
    appendUserBubble(text);

    // 2. Show Typing Indicator
    if (typingIndicator) {
      typingIndicator.style.display = 'flex';
      if (chatStream) chatStream.scrollTop = chatStream.scrollHeight;
    }

    // 3. Process Response
    try {
      const apiKey = localStorage.getItem('sb_gemini_api_key');
      const model = localStorage.getItem('sb_gemini_model') || 'gemini-2.0-flash';

      // Check Local Sunnah KB first for instant verified answers
      const localAnswer = findSunnahLocalAnswer(text);
      if (localAnswer) {
        await new Promise(r => setTimeout(r, 400)); // slight natural pause
        if (typingIndicator) typingIndicator.style.display = 'none';
        appendBotBubble(localAnswer, text);
        return;
      }

      // If user has API Key, call Direct Gemini API
      if (apiKey) {
        const directResp = await callDirectGeminiAPI(apiKey, model, text);
        if (typingIndicator) typingIndicator.style.display = 'none';
        if (directResp) {
          appendBotBubble(directResp, text);
          return;
        }
      }

      // Try Supabase edge function
      const edgeResp = await callSupabaseEdgeFunction(text);
      if (typingIndicator) typingIndicator.style.display = 'none';
      if (edgeResp) {
        appendBotBubble(edgeResp, text);
        return;
      }

      // If no API Key and not in local KB: Offer rich Sunnah response with 1-click Gemini Web
      if (typingIndicator) typingIndicator.style.display = 'none';
      const fallbackMsg = `وعليكم السلام ورحمة الله وبركاته.

سؤالك المبارك: <strong>«${text}»</strong>.

💡 <strong>للحصول على إجابة شرعية فورية وموسعة بالذكاء الاصطناعي:</strong>
يمكنك الضغط مباشرة على زر <strong>(🌐 فتح في متصفح Gemini)</strong> بالأسفل، وسيقوم التطبيق بنسخ سؤالك مؤطراً بمنهج أهل السنة والجماعة وفتح متصفح Google Gemini الرسمي لك فوراً دون الحاجة لإدخال أي مفتاح.

🔑 أو يمكنك إضافة مفتاحك المجاني من <em>Google AI Studio</em> في <strong>(إعدادات مفتاح API)</strong> بالأعلى لتتم المحادثة المباشرة هنا داخل التطبيق.`;

      appendBotBubble(fallbackMsg, text, true);

    } catch (e) {
      if (typingIndicator) typingIndicator.style.display = 'none';
      appendBotBubble(`حدث خطأ أثناء معالجة السؤال. يمكنك دائماً الضغط على زر <strong>(فتح في متصفح Gemini)</strong> لبحث السؤال هناك مباشرة بنقرة واحدة.`, text, true);
    }
  }

  // Direct Gemini API Call with Sunnah System Instructions
  async function callDirectGeminiAPI(apiKey, model, question) {
    try {
      const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
      const payload = {
        system_instruction: {
          parts: [{ text: SUNNAH_SYSTEM_PROMPT }]
        },
        contents: [
          {
            role: 'user',
            parts: [{ text: question }]
          }
        ],
        generationConfig: {
          temperature: 0.3,
          maxOutputTokens: 2048
        }
      };

      const resp = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!resp.ok) return null;
      const data = await resp.json();
      return data.candidates?.[0]?.content?.parts?.[0]?.text || null;
    } catch (e) {
      console.warn('Direct Gemini call failed:', e);
      return null;
    }
  }

  // Supabase Edge Function Call
  async function callSupabaseEdgeFunction(question) {
    try {
      const endpoint = 'https://hywewsfxlxouxjpbkmgr.supabase.co/functions/v1/quran-tadabbur';
      const anonKey = 'sb_publishable_sgbTWOV4y_2V-8Hfr_Hu5A_N_9ZCZ6M';
      const resp = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'apikey': anonKey,
          'Authorization': `Bearer ${anonKey}`
        },
        body: JSON.stringify({
          prompt: `وفق معتقد ومنهج أهل السنة والجماعة: ${question}`,
          history: []
        }),
        signal: AbortSignal.timeout(6000)
      });
      if (!resp.ok) return null;
      const data = await resp.json();
      return data.result || data.text || data.response || null;
    } catch (e) {
      return null;
    }
  }

  // Append User Bubble
  function appendUserBubble(text) {
    if (!chatStream) return;
    const row = document.createElement('div');
    row.className = 'chat-msg-row user-row';
    row.innerHTML = `
      <div class="chat-avatar user-avatar">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
      </div>
      <div class="chat-bubble user-bubble">
        <div class="bubble-content">${escapeHtml(text)}</div>
      </div>
    `;
    chatStream.appendChild(row);
    chatStream.scrollTop = chatStream.scrollHeight;
  }

  // Append Bot Bubble
  function appendBotBubble(responseText, originalQuestion, showGeminiHighlight = false) {
    if (!chatStream) return;
    const row = document.createElement('div');
    row.className = 'chat-msg-row bot-row';

    const formattedContent = renderMessageContentHtml(responseText);

    row.innerHTML = `
      <div class="chat-avatar bot-avatar">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2 2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zm-9 6a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4v10a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V8zm7 4.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3zm4 0a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3zM9 16h6a1 1 0 0 0 0-2H9a1 1 0 0 0 0 2z"/></svg>
      </div>
      <div class="chat-bubble bot-bubble">
        <div class="bubble-header">
          <span class="sender-name">المساعد الشرعي للتدبر</span>
          <span class="sunnah-tag">أهل السنة والجماعة</span>
        </div>
        <div class="bubble-content">${formattedContent}</div>
        <div class="bubble-actions-toolbar">
          <button class="btn-bubble-tool btn-copy-reply" title="نسخ نص الإجابة">
            <span>📋 نسخ الإجابة</span>
          </button>
          <button class="btn-bubble-tool btn-speak-reply" title="قراءة صوتية للإجابة">
            <span>🔊 استماع</span>
          </button>
          <button class="btn-bubble-tool btn-gemini-tool ${showGeminiHighlight ? 'gemini-highlight' : ''}" title="فتح هذا السؤال مؤطراً بالسنة في متصفح Google Gemini">
            <span>🌐 فتح في متصفح Gemini</span>
          </button>
        </div>
      </div>
    `;

    // Hook bubble tools
    const btnCopy = row.querySelector('.btn-copy-reply');
    if (btnCopy) {
      btnCopy.addEventListener('click', () => {
        const plainText = responseText.replace(/<[^>]+>/g, '');
        navigator.clipboard.writeText(plainText).then(() => {
          showAppToast('تم نسخ نص الإجابة إلى الحافظة بنجاح!', 'success');
        });
      });
    }

    const btnSpeak = row.querySelector('.btn-speak-reply');
    if (btnSpeak) {
      btnSpeak.addEventListener('click', () => {
        speakTextArabic(responseText);
      });
    }

    const btnGemini = row.querySelector('.btn-gemini-tool');
    if (btnGemini) {
      btnGemini.addEventListener('click', () => {
        openInGeminiWeb(originalQuestion);
      });
    }

    chatStream.appendChild(row);
    chatStream.scrollTop = chatStream.scrollHeight;
  }

  // Hook Send Button
  if (sendBtn) sendBtn.addEventListener('click', sendMessage);

  // Global function for external calls (e.g. from Quran verse options)
  window.sendAssistantUserMessage = function() {
    sendMessage();
  };
}

// Simple HTML escaper
function escapeHtml(text) {
  const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
  return text.replace(/[&<>"']/g, m => map[m]);
}

const setupAssistant = setupIslamicAIAssistant;

// =========================================================================
// 8. Privacy Policy Modal Manager (نافذة سياسة الخصوصية وأمان البيانات)
// =========================================================================
function setupPrivacyPolicy() {
  const modal = document.getElementById('modalPrivacyPolicy');
  const closeBtn = document.getElementById('btnClosePrivacyPolicyModal');
  const confirmBtn = document.getElementById('btnConfirmClosePrivacy');
  const cardHome = document.getElementById('cardHomePrivacyPolicy');
  const sidebarBtn = document.getElementById('btnSidebarPrivacyPolicy');

  function openPrivacy() {
    if (modal) {
      modal.classList.add('active');
    }
  }

  function closePrivacy() {
    if (modal) {
      modal.classList.remove('active');
    }
  }

  if (cardHome) cardHome.addEventListener('click', openPrivacy);
  if (sidebarBtn) sidebarBtn.addEventListener('click', openPrivacy);
  if (closeBtn) closeBtn.addEventListener('click', closePrivacy);
  if (confirmBtn) confirmBtn.addEventListener('click', closePrivacy);

  if (modal) {
    modal.addEventListener('click', (e) => {
      if (e.target === modal) closePrivacy();
    });
  }

  document.querySelectorAll('[data-open-privacy]').forEach(el => {
    el.addEventListener('click', (e) => {
      e.preventDefault();
      openPrivacy();
    });
  });

  window.openPrivacyPolicyModal = openPrivacy;
}


