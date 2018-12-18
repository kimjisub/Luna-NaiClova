function success() {
    let msg = new SpeechSynthesisUtterance('성공적');
    msg.lang = 'ko-KR';
    speechSynthesis.speak(msg);
}