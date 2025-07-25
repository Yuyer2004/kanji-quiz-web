let currentKanji = "";
let score = 0, total = 0;
let isAnswered = false;
let isQuizFinished = false;
const level = new URLSearchParams(window.location.search).get("level") || "normal";

document.getElementById("title").textContent = `漢字クイズ`;

async function loadQuestion() {
  document.getElementById("result").textContent = "";
  document.getElementById("meaning").textContent = "";
  document.getElementById("answer").value = "";
  document.getElementById("answer").disabled = false;
  isAnswered = false;

  const btn = document.querySelector("button");
  btn.textContent = "送信";
  btn.onclick = submitAnswer;

  try {
    const res = await fetch(`/api/kanji?level=${level}`, { cache: "no-store" });
    const text = await res.text();
    if (!text) return finishQuiz();

    const data = JSON.parse(text);
    if (!data || !data.kanji) return finishQuiz();

    currentKanji = data.kanji;
    document.getElementById("kanji").textContent = currentKanji;
    document.getElementById("answer").focus();
  } catch (e) {
    console.error("loadQuestion() error:", e);
    finishQuiz();
  }
}

async function finishQuiz() {
  document.getElementById("kanji").textContent = "終了！";
  document.getElementById("answer").style.display = "none";
  document.querySelector("button").style.display = "none";
  document.getElementById("result").textContent =
    `あなたのスコアは ${score} / ${total}（正答率 ${Math.round((score / total) * 100)}%）`;

  const retryBtn = document.createElement("button");
  retryBtn.textContent = "もう一度やる";
  retryBtn.onclick = async () => {
    await fetch(`/api/reset?level=${level}`, { method: "POST" });
    location.reload();
  };
  document.getElementById("stats").appendChild(retryBtn);
  isQuizFinished = true;

  try {
    const res = await fetch(`/api/finish?level=${level}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ score, total })
    });
    const data = await res.json();
    const statsDiv = document.createElement("div");
    statsDiv.textContent = `あなたは ${data.rank} 位 / ${data.totalPlayers} 人中！`;
    document.getElementById("stats").appendChild(statsDiv);
  } catch (e) {
    console.error("スコア送信エラー:", e);
  }
}

async function submitAnswer() {
  if (isAnswered || isQuizFinished) return;

  const userAnswer = document.getElementById("answer").value.trim();
  if (!userAnswer) return;

  const res = await fetch("/api/check", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ kanji: currentKanji, answer: userAnswer })
  });

  const data = await res.json();
  total++;
  isAnswered = true;
  document.getElementById("answer").disabled = true;

  if (data.correct) {
    score++;
    document.getElementById("result").textContent = "⭕ 正解！";
  } else {
    document.getElementById("result").textContent = `❌ 不正解！ 正答: ${data.correctReading || '？'}`;
  }

  document.getElementById("meaning").textContent = `意味: ${data.meaning || '—'}`;
  updateStats();

  const btn = document.querySelector("button");
  btn.textContent = "次へ";
  btn.onclick = loadQuestion;
}

function updateStats() {
  document.getElementById("score").textContent = score;
  document.getElementById("total").textContent = total;
  document.getElementById("accuracy").textContent =
    total > 0 ? Math.round((score / total) * 100) : 0;
}

window.onload = function () {
  const level = new URLSearchParams(window.location.search).get("level") || "normal";
  const container = document.getElementById("container");

  switch (level) {
    case "easy":
      container.style.backgroundColor = "#deffd9"; 
      break;
    case "normal":
      container.style.backgroundColor = "#fde1b0"; 
      break;
    case "hard":
      container.style.backgroundColor = "#ffd3d3"; 
      break;
  }

  loadQuestion();
  document.addEventListener("keydown", function (e) {
    if (e.key === "Enter") {
      if (isQuizFinished) return;
      if (!isAnswered) submitAnswer();
      else loadQuestion();
    }
  });
};
