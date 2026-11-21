const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_shinobi_123";
const BOB_UID = "bob_shinobi_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any user profile or subcollection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set({
      userId: ALICE_UID,
      title: "Rogue Mission",
      category: "STUDY",
      dateIso: "2026-11-20",
      startTimeMinutes: 540,
      endTimeMinutes: 600,
      priority: "HIGH",
      missionRank: "B",
      recurrence: "NONE",
      xpReward: 100,
      colorHex: "#00E5FF",
      completed: false,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create and partially update UserProfile; rejects ghost fields and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Kira Scholar",
      username: "kira_shadow",
      email: "kira@example.com",
      university: "Addis Ababa University",
      department: "Computer Science",
      academicYear: "Year 3",
      xp: 250,
      level: 2,
      ninjaTitle: "Shadow Scholar",
      currentStreak: 3,
      longestStreak: 5,
      onboardingCompleted: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 350,
      level: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt change rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 400,
      updatedAt: new Date(Date.now() + 120000),
    })
  );
});

test("Owner can create, list, update, and delete tasks; rejects @DocumentId id field and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      title: "Algorithms Lab Prep",
      notes: "Review dynamic programming",
      subjectName: "CS301",
      category: "STUDY",
      dateIso: "2026-11-21",
      startTimeMinutes: 600,
      endTimeMinutes: 720,
      priority: "HIGH",
      missionRank: "A",
      recurrence: "WEEKLY",
      location: "Engineering Block B",
      reminderOffsets: "0,10,30",
      xpReward: 150,
      colorHex: "#00E5FF",
      completed: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Subcollection list succeeds for owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").get()
  );

  // Partial completion update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").update({
      completed: true,
      completedAtIso: "2026-11-21T10:00:00",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject extra @DocumentId 'id' field on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_2").set({
      id: "task_2",
      userId: ALICE_UID,
      title: "Invalid ID payload",
      category: "STUDY",
      dateIso: "2026-11-21",
      startTimeMinutes: 600,
      endTimeMinutes: 660,
      priority: "MEDIUM",
      missionRank: "C",
      recurrence: "NONE",
      xpReward: 50,
      colorHex: "#FF6D00",
      completed: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").update({
      title: "Tampered",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").delete()
  );
});

test("Owner can manage subjects, studySessions, settings, chats, chatMessages, notes, achievements, and backups", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_1").set({
      userId: ALICE_UID,
      name: "Linear Algebra",
      code: "MATH201",
      instructor: "Dr. Tadesse",
      room: "Hall 4",
      colorHex: "#00E5FF",
      targetHoursPerWeek: 6,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("studySessions").doc("sess_1").set({
      userId: ALICE_UID,
      subjectName: "Linear Algebra",
      mode: "POMODORO",
      durationMinutes: 25,
      xpEarned: 60,
      startedAtEpochMs: Date.now() - 1500000,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("prefs").set({
      userId: ALICE_UID,
      themePreset: "CYBER_SHINOBI",
      appearanceMode: "DARK",
      timezoneId: "Africa/Addis_Ababa",
      use24HourClock: false,
      useEthiopianCalendar: true,
      showDualCalendar: true,
      aiPersonality: "MENTOR",
      animationsEnabled: true,
      reduceMotion: false,
      hapticsEnabled: true,
      floatingAiEnabled: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chats").doc("chat_1").set({
      userId: ALICE_UID,
      title: "Week Strategy",
      personality: "MENTOR",
      lastMessagePreview: "Let us plan tomorrow's study blocks.",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chatMessages").doc("msg_1").set({
      userId: ALICE_UID,
      chatId: "chat_1",
      role: "user",
      content: "Add math study tomorrow at 4 PM",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Eigenvalues Summary",
      content: "Characteristic polynomial det(A - lambda I) = 0",
      subjectName: "Linear Algebra",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("achievements").doc("ach_1").set({
      userId: ALICE_UID,
      title: "First Scroll",
      description: "Complete your first study mission",
      badgeCode: "FIRST_SCROLL",
      unlocked: true,
      xpBonus: 100,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("backups").doc("bk_1").set({
      userId: ALICE_UID,
      summary: "Manual Cloud Scroll Backup",
      taskCount: 5,
      xpSnapshot: 450,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Cross-user read denied on all subcollections
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("chats").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("settings").doc("prefs").get());
});
