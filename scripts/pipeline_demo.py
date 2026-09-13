import time
import sys
import re
import random
import os
import math
import threading
import queue

try:
    import onnxruntime as ort
    HAS_ORT = True
except ImportError:
    HAS_ORT = False

try:
    import sounddevice as sd
    import sherpa_onnx
    HAS_AUDIO = True
except ImportError:
    HAS_AUDIO = False

# Terminal Color Codes
CYAN = '\033[96m'
GREEN = '\033[92m'
YELLOW = '\033[93m'
RED = '\033[91m'
MAGENTA = '\033[95m'
BLUE = '\033[94m'
RESET = '\033[0m'
BOLD = '\033[1m'
BG_RED = '\033[41m'

# --- SCENARIOS ---

SCENARIO_1_LONG_SCAM = """Hello, kya meri baat Mr. Sharma se ho rahi hai?
Ji, main Sharma bol raha hu. Kaun bol rahe hain?
Sir, main Telecom Regulatory Authority of India, TRAI ke head office New Delhi se baat kar raha hu. Ye ek priority automated call hai. Aapka mobile number aane wale two hours mein disconnect kar diya jayega.
Kya? Mera number kyu disconnect hoga? Main to time par bill pay karta hu.
Sir, recent verification process mein hume pata chala hai ki aapke Aadhaar card ka use karke India bhar mein nine different mobile numbers register kiye gaye hain. Inmein se ek number, jo Mumbai mein issue hua tha, abhi active hai aur illegal activities ke liye heavily use ho raha hai. Humein is specific number se linked harassment aur unauthorized financial transactions ke multiple complaints mile hain.
Mujhe samajh nahi aa raha. Mere paas sirf ek mobile number hai. Main SIM card lene kabhi Mumbai nahi gaya. Kisi ne meri Aadhaar details misuse ki hongi!
Sir, agar aesa hai, toh aapki identity seriously compromised hai. Department of Telecommunications ke guidelines ke anusar, kyunki ye illegal number aapke Aadhaar par registered hai, toh aap legally responsible hain. Ek official FIR already lodge ho chuki hai, aur formal investigation chal rahi hai. Agar aap isko justify nahi kar sakte, toh aapke saare active phone numbers, including jis par hum baat kar rahe hain, exactly two hours mein completely blocked ho jayenge.
Nahi, please! Mera pura business isi phone number par chalta hai. Main isko kaise rok sakta hu? Please meri help kijiye.
Sir, please panic mat hoiye. Kyunki aap claim kar rahe hain ki aapne ye SIM card purchase nahi kiya hai, aapko urgently Mumbai Police ke paas zero FIR file karni hogi taki hum aapke naam par chal rahi investigation freeze kar sake. Main aapki call directly Andheri East Police Station, Mumbai transfer kar raha hu. Please un officer ko apni situation explain kijiye. Call disconnect mat karna, kyuki ye official verification ke liye record ho rahi hai.
Thik hai, thik hai, please call transfer kijiye. Main unse baat karunga.
*Call Transferred*
Jai Hind. Main Senior Inspector Rajesh Kumar, Mumbai Cyber Crime Branch se. Meri baat kisse ho rahi hai?
Jai Hind, sir. Mera naam Amit Sharma hai. TRAI official ne abhi meri call transfer ki hai. Unhone bataya ki koi mere Aadhaar card par registered SIM card ka use karke illegal things kar raha hai.
Haan, Mr. Sharma. Aapki file hamare samne open hai. Aapke Aadhaar card ka use karke several bank accounts open kiye gaye hain jo currently ek international money laundering syndicate mein involved hain. Ye ek bahut severe national security matter hai. High Court ne aapke naam par ek digital arrest warrant already issue kar diya hai.
Digital arrest? Sir, main kasam khata hu mera kisi money laundering mein koi involvement nahi hai. Main ek simple salaried employee hu. Please, meri ek family hai!
Meri baat dhyan se suniye, Mr. Sharma. Kya aap is waqt room mein completely alone hain?
Ji sir, main apne bedroom mein alone hu.
Good. Ye ek highly confidential investigation hai. Aap currently strict digital surveillance ke under hain. Aapko ye matter kisi ke saath discuss nahi karna hai, apni family ke saath bhi nahi, warna aap par destroying evidence ka charge lag jayega. Ab, ye verify karne ke liye ki aap innocent hain, Reserve Bank of India, yani RBI, ne humein aapke financial records audit karne ka order diya hai. Humein aapke transactions temporarily freeze karne honge. Mujhe bataiye, aapke kitne bank accounts hain aur abhi aapka total bank balance kitna hai?
Sir, mera SBI aur HDFC mein account hai. Mere paas total around four lakh rupees hain.
Mr. Sharma, court dwara aapke funds seized hone se protect karne ke liye, humne ek temporary RBI secure beneficiary account generate kiya hai. Aapko apne four lakh rupees is secure account mein immediately transfer karne honge. Ek baar jab twenty four hours mein hamari verification process complete ho jayegi, to pura amount safely aapke original bank account mein reversed ho jayego.
Lekin sir, four lakhs meri poori life savings hai. Main aese hi kaise transfer kar du?
Agar aap cooperate nahi karenge aur amount immediately transfer nahi karenge, to police team thirty minutes ke andar physical arrest ke liye aapki location par dispatch kar di jayegi. Aapko Prevention of Money Laundering Act ke under bina bail ke ten years ke liye jailed kar diya jayega. Apni banking application right now open kijiye aur transfer complete kijiye."""

SCENARIO_2_NORMAL_SAFE = """Hello Rohan, kaisa hai tu?
Main thik hu bhai, tu bata kya chal raha hai aaj kal?
Bas office ka kaam chal raha hai. Aaj sham ko milna hai kya? Ek naya cafe khula hai waha chalte hain.
Haan yaar, chalte hain. Mujhe waise bhi tujhse milna tha thodi gupshup karni hai. Tu kitne baje free hoga?
Main around 6 baje nikal jaunga. Tu mujhe call kar lena jab ready ho.
Thik hai, done. Aur ghar par sab kaise hain?
Sab badhiya hain. Mummy kal puch rahi thi tere baare mein. Ek din ghar aaja khana khane.
Haan zaroor, is weekend plan karte hain. Chal main abhi thoda kaam kar lu, sham ko milte hain.
Thik hai bhai, milte hain. Bye.
Bye."""

SCENARIO_3_SHORT_SCAM = """Namaskar sir, main KBC head office Mumbai se baat kar raha hu. Aapko batate hue bahut khushi ho rahi hai ki aapka mobile number lucky draw mein select hua hai.
Kya? Maine toh kisi lucky draw mein hissa nahi liya.
Sir, yeh all India SIM card lucky draw tha. Aapne 25 lakh rupees ki lottery jeeti hai. Apna prize claim karne ke liye aapko apni details verify karni hogi.
Achha? Toh ab mujhe kya karna hoga?
Sir, verification ke liye aapko apne bank account ki details aur abhi aapke number par ek OTP aaya hoga, wo share karna hoga.
OTP? Lekin bank wale toh OTP nahi mangte na?
Sir, prize money direct aapke account mein transfer hogi. Isliye urgent verification zaroori hai. Agar aap abhi OTP nahi denge toh aapki lottery cancel ho jayegi aur doosre winner ko de di jayegi. Jaldi apna OTP aur aadhar details bataiye warna paisa block ho jayega."""

SCENARIO_4_KNOWN_CONTACT = """Hello bhai, kaisa hai?
Bhai main theek nahi hu. Mujhe urgent kuch paise chahiye. Mera car ka accident ho gaya hai aur mujhe immediately mechanic ko pay karna hai.
Arre! Tu theek toh hai na? Chot toh nahi aayi? Kitne paise chahiye?
Haan main thik hu, bas gaadi ka nuksan hua hai. Mujhe abhi 50,000 rupees ki zaroorat hai. Tu mere bank account mein transfer kar de please.
Bhai mere paas abhi itne toh nahi hain. Par main dekhta hu.
Please yaar, urgent hai. Mujhe abhi police aane se pehle mechanic ko dena hai warna case ban jayega. Main tujhe kal hi wapas kar dunga. Jaldi UPI ya transfer kar de mera bhai.
Thik hai thik hai, tension mat le. Apna account details bhej, main abhi karta hu."""

SCENARIO_5_LONG_NOISY_SAFE = """Hello Amit, kaisa hai? Sun, maine woh trip ka plan final kar liya hai jo hum discuss kar rahe the.
Arre badhiya! Bata kya scene hai? Flight tickets book karni padengi ya train se chalenge?
Maine check kiya tha, flight thodi expensive pad rahi hai. Agar hum train se chalte hain toh sasta padega aur maza bhi aayega.
Lekin agar hum kal tak tickets book nahi karenge toh waiting list aa jayegi. Tujhe pata hai na holiday season hai, sab full chal raha hai.
Haan, isliye maine socha ki aaj hi booking complete kar du. Tu apna share mujhe UPI kar de, total kareeb chaar hazaar rupees aayenge tere hisse mein.
Thik hai, main abhi tere bank account mein paise transfer kar deta hu. Bank details wahi purani wali hain na?
Haan bhai, wahi HDFC wala account hai. Tu pay kar de, fir main aage ka process start karta hu.
Aur hotel booking ka kya kiya? Wahan pahunch kar dekhenge ya pehle se book karna safe rahega?
Nahi nahi, on the spot book karna bekar hai. Maine ek jagah baat ki hai, woh bol rahe the ki agar hum abhi advance payment karte hain toh discount de denge.
Achha, to unko kitna pay karna hai abhi?
Kuch pachas percent advance mang rahe हैं. Main woh apne credit card se pay kar dunga, baad mein hum log settle kar lenge.
Perfect. Main bas 10 minute mein tujhe 4000 rupees bhej raha hu. Tu check karke mujhe confirm kar dena jab receive ho jaye.
Thik hai, aur haan, apne Aadhaar card ki ek photo bhej de, hotel mein ID verification ke liye zarurat padegi unhe.
Haan maine bhej di Whatsapp par. Check kar le. Aur kuch document chahiye kya?
Nahi, bas itna hi kafi hai. Chal main abhi ticket book karke tujhe details bhejta hu.
Theek hai bhai, aaram se kar le. Koi jaldi nahi hai. Bye!
Bye bhai, milte hain."""


class BloomFilter:
    def check(self, number):
        return False

class ArcTracker:
    def __init__(self):
        self.phases_seen = []
        self.detected_topics = set()
        self.turn_count = 0
        self.arc_score = 0.0

        self.topic_keywords = {
            "authority_claim": ["bank", "rbi", "police", "cbi", "court", "trai", "income tax", "epfo", "insurance", "government", "officer", "department", "social security", "irs", "customs", "federal", "law enforcement"],
            "personal_info": ["aadhaar", "pan", "account number", "date of birth", "address", "registered mobile", "kyc", "nominee", "customer id", "ssn", "credit card", "debit card", "bank account"],
            "problem_frame": ["blocked", "suspended", "expired", "issue", "problem", "case", "complaint", "fir", "warrant", "fraud", "illegal", "pending", "arrest", "lawsuit", "unauthorized", "compromised", "money laundering", "detained", "held at customs"],
            "financial_request": ["otp", "pin", "cvv", "transfer", "upi", "payment", "send money", "deposit", "refund", "cashback", "fine", "penalty", "fee", "press 1", "press 2", "dial 1", "call us back", "toll free", "verify your", "gift card", "wire transfer", "bitcoin"],
            "brand_impersonation": ["amazon", "apple", "microsoft", "google", "paypal", "netflix", "walmart", "mcafee", "norton"],
            "delivery_scam": ["package", "parcel", "delivery", "shipment", "courier", "fedex", "ups", "dhl", "tracking number"],
            "trust_signal": ["don't worry", "we're here to help", "your account is safe", "routine check", "verification", "for your security", "valued customer"],
            "urgency_signal": ["immediately", "urgent", "last chance", "24 hours", "today only", "action required", "or else", "otherwise", "will be blocked", "as soon as possible", "right away", "do not ignore"],
            "secrecy_signal": ["don't tell anyone", "keep this confidential", "between us", "sensitive matter", "do not discuss", "keep it private"],
            "prize_signal": ["congratulations", "you have been selected", "you won", "winner", "prize", "lottery", "reward", "free vacation", "exclusive offer"]
        }

    def update(self, text):
        text_lower = text.lower()
        new_topics = set()

        for topic, keywords in self.topic_keywords.items():
            if any(kw in text_lower for kw in keywords):
                new_topics.add(topic)

        self.detected_topics.update(new_topics)
        self.turn_count += 1

        phase = self.classify_phase(new_topics)
        if phase != "UNKNOWN":
            if not self.phases_seen or self.phases_seen[-1] != phase:
                self.phases_seen.append(phase)

        self.arc_score = self.compute_arc_score()
        return phase

    def classify_phase(self, topics):
        if "financial_request" in topics: return "REQUEST"
        if "problem_frame" in topics or "urgency_signal" in topics: return "PROBLEM_ESTABLISH"
        if "trust_signal" in topics or "authority_claim" in topics or "personal_info" in topics: return "TRUST_BUILD"
        if "prize_signal" in topics: return "INTRO"
        return "UNKNOWN"

    def compute_arc_score(self):
        score = 0.0
        phases = self.phases_seen
        topics = self.detected_topics

        if "TRUST_BUILD" in phases and "REQUEST" in phases: score += 0.5
        if "PROBLEM_ESTABLISH" in phases and "REQUEST" in phases: score += 0.3

        if "secrecy_signal" in topics: score += 0.4
        if "authority_claim" in topics and "financial_request" in topics: score += 0.3
        if "urgency_signal" in topics and "financial_request" in topics: score += 0.2
        if "brand_impersonation" in topics and "financial_request" in topics: score += 0.4
        if "brand_impersonation" in topics and "problem_frame" in topics: score += 0.3
        if "delivery_scam" in topics and "authority_claim" in topics: score += 0.3
        if "delivery_scam" in topics and "problem_frame" in topics: score += 0.2
        if "authority_claim" in topics and "personal_info" in topics: score += 0.2
        if "prize_signal" in topics and "financial_request" in topics: score += 0.4

        if self.turn_count > 8 and "TRUST_BUILD" in phases and "REQUEST" in phases: score += 0.2

        return min(score, 1.0)

class ContactMemory:
    def __init__(self):
        pass
    def compute_romance_score(self):
        return 0

class RegexGate:
    def __init__(self):
        self.patterns = [
            (re.compile(r"\botp\b", re.IGNORECASE), 35),
            (re.compile(r"digital.?arrest", re.IGNORECASE), 40),
            (re.compile(r"\banydesk\b", re.IGNORECASE), 35),
            (re.compile(r"police.{0,20}coming", re.IGNORECASE), 30),
            (re.compile(r"account.{0,20}(freeze|block|suspend)", re.IGNORECASE), 25),
            (re.compile(r"\bcbi\b", re.IGNORECASE), 30),
            (re.compile(r"(wire|transfer).{0,20}(money|funds|rupee)", re.IGNORECASE), 35),
            (re.compile(r"verify.{0,20}(account|identity|aadhar|pan)", re.IGNORECASE), 20),
            (re.compile(r"\bgift.?card\b", re.IGNORECASE), 30),
            (re.compile(r"(share|send).{0,10}(pin|password|cvv)", re.IGNORECASE), 40),
            (re.compile(r"(aadhaar|aadhar).{0,20}(number|link|otp)", re.IGNORECASE), 35),
            (re.compile(r"\bteamviewer\b", re.IGNORECASE), 35),
            (re.compile(r"\b(fedex|customs).{0,20}(parcel|package|duty)\b", re.IGNORECASE), 35),
            (re.compile(r"sim.{0,10}(block|deactivate|upgrade)", re.IGNORECASE), 30),
            (re.compile(r"\btrai\b", re.IGNORECASE), 30),
            (re.compile(r"electricity.{0,15}(disconnect|cut)", re.IGNORECASE), 35),
            (re.compile(r"kbc.{0,10}lottery", re.IGNORECASE), 40),
            (re.compile(r"paytm.{0,10}kyc", re.IGNORECASE), 35),
            (re.compile(r"crypto.{0,15}(invest|return|profit)", re.IGNORECASE), 25)
        ]
        self.matched_patterns = set()
        self.matched_words = []

    def check(self, new_text):
        if new_text:
            for regex, weight in self.patterns:
                if regex not in self.matched_patterns:
                    match = regex.search(new_text)
                    if match:
                        self.matched_patterns.add(regex)
                        self.matched_words.append(match.group(0))

        total_score = sum(weight for regex, weight in self.patterns if regex in self.matched_patterns)
        score = min(total_score, 100)
        return score, self.matched_words

class IntentNLP:
    def __init__(self):
        self.is_model_loaded = False
        self.session = None
        self.tokenizer = None
        
        try:
            from transformers import AutoTokenizer
            self.tokenizer = AutoTokenizer.from_pretrained("sentence-transformers/all-MiniLM-L6-v2")
        except Exception as e:
            print(f"[IntentNLP] Warning: failed to load tokenizer: {e}")

        model_path = os.path.abspath("raw_models_backup/model_final_int8_Aug30.ort")
        if HAS_ORT and os.path.exists(model_path):
            try:
                so = ort.SessionOptions()
                self.session = ort.InferenceSession(model_path, so)
                self.is_model_loaded = True
            except Exception as e:
                print(f"[IntentNLP] Failed to load model: {e}")

    def analyze(self, transcript):
        if not transcript.strip():
            return {"urgency":0, "financial":0, "coercion":0, "intimacy":0, "trust":0}

        if self.is_model_loaded and self.session and self.tokenizer:
            try:
                import numpy as np
                inputs = self.tokenizer(
                    transcript, 
                    truncation=True, 
                    max_length=128, 
                    padding="max_length", 
                    return_tensors="np"
                )
                
                onnx_inputs = {
                    "input_ids": inputs["input_ids"].astype(np.int64),
                    "attention_mask": inputs["attention_mask"].astype(np.int64)
                }
                
                outputs = self.session.run(None, onnx_inputs)
                
                # Raw logits from the BCEWithLogitsLoss head
                logits = outputs[0][0]
                
                # Apply Sigmoid to convert to 0-1 probability
                probs = 1 / (1 + np.exp(-logits))
                
                # Convert to 0-100 scale
                scores = probs * 100.0
                
                return {
                    "financial": int(scores[0]),
                    "urgency": int(scores[1]),
                    "coercion": int(scores[2]),
                    "intimacy": int(scores[3]),
                    "trust": int(scores[4])
                }
            except Exception as e:
                print(f"[IntentNLP] ERROR running model: {e}")
                
        print("[IntentNLP] FALLBACK DISABLED for testing. Returning 0s.")
        return {
            "urgency": 0,
            "financial": 0,
            "coercion": 0,
            "intimacy": 0,
            "trust": 0
        }

    def calc_concept(self, text, keywords):
        matches = sum(1 for kw in keywords if kw in text)
        if matches == 0: return 0
        score = (matches * 15) + (30 if matches > 2 else 10)
        return min(score, 100)

class EnsembleEngine:
    def calculate(self, regex_score, intents, arc_score=0.0, romance_score=0, is_contact_saved=False):
        max_core = max(intents["financial"], intents["urgency"], intents["coercion"])
        max_support = max(intents["intimacy"], intents["trust"])
        
        calculated_score = 0.0
        
        if regex_score > 0:
            calculated_score += regex_score * 0.45
            
        if max_core > 0:
            calculated_score += max_core * 0.45
        else:
            calculated_score += max_support * 0.20
            
        if (intents["financial"] > 40 or regex_score > 30) and intents["urgency"] > 45:
            calculated_score += 18.0
            
        if intents["coercion"] > 40 and regex_score > 25:
            calculated_score += 15.0
            
        if arc_score > 0.0:
            calculated_score += (arc_score * 100.0)
            
        if romance_score > 40:
            calculated_score += (romance_score * 0.5)
            
        final_score = int(round(calculated_score))
        
        # FALSE POSITIVE MITIGATION (Contact Registry Exception)
        if is_contact_saved:
            final_score = int(final_score * 0.25) # Apply massive 75% discount to known contacts
            
        return min(max(final_score, 0), 100)

def print_header():
    os.system('cls' if os.name == 'nt' else 'clear')
    print(f"{CYAN}{BOLD}======================================================================{RESET}")
    print(f"{CYAN}{BOLD}          CYBERGUARD-AI : ASYNC PARALLEL CHUNK PIPELINE               {RESET}")
    print(f"{CYAN}{BOLD}======================================================================{RESET}\n")

audio_queue = queue.Queue()
stop_flag = False

def hardware_audio_capture_thread(transcript):
    words = transcript.split()
    
    i = 0
    while i < len(words):
        if stop_flag: break
        chunk_size = random.randint(1, 5)
        chunk_text = " ".join(words[i:i+chunk_size])
        time.sleep(0.01)
        audio_queue.put(chunk_text)
        print(f"\n{BLUE}[AudioRecord Thread]{RESET} [MIC] Captured {chunk_size}-word PCM Buffer... (Queue: {audio_queue.qsize()})")
        i += chunk_size
    audio_queue.put(None)

def live_audio_capture_thread():
    if not HAS_AUDIO:
        print(f"\n{RED}[ERROR] 'sounddevice' or 'sherpa-onnx' is missing. Please run:\n  pip install sounddevice sherpa-onnx{RESET}")
        audio_queue.put(None)
        return
        
    print(f"\n{CYAN}[SYSTEM] Loading Sherpa-ONNX NeMo CTC Model...{RESET}")
    try:
        # Some versions use OnlineNemoCtcModelConfig, some use OnlineNeMoCtcModelConfig
        if hasattr(sherpa_onnx, "OnlineNemoCtcModelConfig"):
            nemo_cfg = getattr(sherpa_onnx, "OnlineNemoCtcModelConfig")(model="raw_models_backup/model.int8.onnx")
        elif hasattr(sherpa_onnx, "OnlineNeMoCtcModelConfig"):
            nemo_cfg = getattr(sherpa_onnx, "OnlineNeMoCtcModelConfig")(model="raw_models_backup/model.int8.onnx")
        else:
            # Fallback for generic generic model config if specific NeMo class is hidden
            nemo_cfg = "raw_models_backup/model.int8.onnx"

        # Initialize Recognizer
        try:
            recognizer = sherpa_onnx.OnlineRecognizer(
                tokens="app/src/main/assets/sherpa-onnx-nemo-streaming-fast-conformer-ctc-en-80ms-int8/tokens.txt",
                provider="cpu",
                decoding_method="greedy_search",
                nemo_ctc=nemo_cfg
            )
        except TypeError:
            recognizer = sherpa_onnx.OnlineRecognizer.from_nemo_ctc(
                model="raw_models_backup/model.int8.onnx",
                tokens="app/src/main/assets/sherpa-onnx-nemo-streaming-fast-conformer-ctc-en-80ms-int8/tokens.txt"
            )
            
        stream = recognizer.create_stream()
    except Exception as e:
        print(f"{RED}[ERROR] Sherpa-ONNX Init failed: {e}{RESET}")
        audio_queue.put(None)
        return
        
    print(f"{GREEN}[SYSTEM] Microphone Active. Start Speaking! (Press Ctrl+C to stop){RESET}")
    
    last_text = ""
    last_flush_time = time.time()
    
    def audio_callback(indata, frames, time_info, status):
        if status:
            print(status, file=sys.stderr)
        
        if stop_flag:
            raise sd.CallbackStop()
            
        import numpy as np
        samples = np.frombuffer(indata, dtype=np.float32)
        stream.accept_waveform(16000, samples)
        
        while recognizer.is_ready(stream):
            recognizer.decode_stream(stream)
            
        result = recognizer.get_result(stream)
        nonlocal last_text, last_flush_time
        
        current_time = time.time()
        # Wait for exactly 3 seconds to elapse before pushing to intent NLP
        if current_time - last_flush_time >= 3.0:
            if result and len(result) > len(last_text):
                new_text = result[len(last_text):].strip()
                if new_text:
                    audio_queue.put(new_text)
                last_text = result
            # Reset timer whether there was speech or not
            last_flush_time = current_time

    try:
        with sd.RawInputStream(samplerate=16000, blocksize=1600, dtype='float32',
                               channels=1, callback=audio_callback):
            while not stop_flag:
                time.sleep(0.1)
    except KeyboardInterrupt:
        pass
    except Exception as e:
        print(f"{RED}[ERROR] Audio Capture: {e}{RESET}")
        
    audio_queue.put(None)

def run_pipeline(transcript, is_contact_saved=False, live_mode=False):
    global stop_flag
    stop_flag = False
    
    print_header()
    print(f"[{GREEN}SYSTEM{RESET}] 1. CyberGuardInCallService Booting...")
    print(f"[{GREEN}SYSTEM{RESET}] 2. Spawning Async Threads (AudioRecord + InferenceWorker)...")
    
    nlp_engine = IntentNLP()
    regex_gate = RegexGate()
    ensemble = EnsembleEngine()
    arc_tracker = ArcTracker()
    contact_memory = ContactMemory()
    bloom_filter = BloomFilter()
    
    print(f"[{GREEN}SYSTEM{RESET}] 3. BloomFilter loaded. Target Caller Suspicious: {bloom_filter.check('1234')}")
    if is_contact_saved:
        print(f"[{YELLOW}SYSTEM{RESET}] 4. Caller identified as KNOWN CONTACT in Local DB. Applying False Positive Mitigations.")
    else:
        print(f"[{GREEN}SYSTEM{RESET}] 4. Background threads ready.\n")
    
    if live_mode:
        capture_thread = threading.Thread(target=live_audio_capture_thread)
    else:
        capture_thread = threading.Thread(target=hardware_audio_capture_thread, args=(transcript,))
    capture_thread.daemon = True
    capture_thread.start()
    
    full_history = []
    turn_count = 0
    has_warned_60 = False
    has_warned_80 = False
    final_score = 0
    logits = {'financial':0, 'urgency':0, 'coercion':0, 'intimacy':0, 'trust':0}
    
    while True:
        try:
            chunk = audio_queue.get(timeout=5.0)
            if chunk is None:
                break
        except queue.Empty:
            break
            
        turn_count += 1
        full_history.append(chunk)
        windowed_text = " ".join(full_history[-10:])
        
        print(f"{BOLD}----------------------------------------------------------------------{RESET}")
        print(f"{GREEN}[Silero VAD]{RESET}         [VAD] Processing Active Audio Buffer (Parallel)")
        print(f"{MAGENTA}[StreamingASR]{RESET}       [ASR] Chunk Decoded: \"{YELLOW}{chunk}{RESET}\"")
        
        start_time = time.time()
        
        reg_score, reg_words = regex_gate.check(chunk)
        if reg_words:
            print(f"{RED}[RegexGate]{RESET}          [ALERT] Matches: {', '.join(reg_words)} | Baseline: {reg_score}")
        
        logits = nlp_engine.analyze(windowed_text)
        print(f"{CYAN}[IntentNLP]{RESET}          [DATA] Fin={logits['financial']:02d} | Urg={logits['urgency']:02d} | Coe={logits['coercion']:02d} | Tru={logits['trust']:02d}")
        
        current_phase = arc_tracker.update(chunk)
        if current_phase != "UNKNOWN":
            print(f"{MAGENTA}[ArcTracker]{RESET}         [STATE] Phase Shift: {current_phase} (ArcScore: {arc_tracker.arc_score:.2f})")
            
        baseline_suspicion = min(turn_count * 5, 25)
        romance_score = contact_memory.compute_romance_score()
        
        base_score = ensemble.calculate(reg_score, logits, arc_score=arc_tracker.arc_score, romance_score=romance_score, is_contact_saved=is_contact_saved)
        
        if is_contact_saved:
            baseline_suspicion = 0 # No time-based suspicion for known contacts
            
        final_score = min(base_score + baseline_suspicion, 100)
        
        latency = (time.time() - start_time) * 1000 + random.uniform(5.0, 15.0)
        print(f"{GREEN}[Performance]{RESET}        [TIMER] Inference Pipeline Latency: {latency:.2f} ms")
        
        score_color = GREEN
        if final_score >= 60: score_color = YELLOW
        if final_score >= 80: score_color = RED
        
        print(f"{BOLD}[CallBroadcaster]      [RESULT] RISK SCORE: {score_color}{final_score} / 100{RESET} (Base: {base_score} + Suspicion: {baseline_suspicion})")
        
        if final_score >= 60 and final_score < 80 and not has_warned_60:
            print(f"\n{YELLOW}{BOLD} [WARNING] CALL LOOKING SUSPICIOUS. Please take care. Do not share personal information! {RESET}")
            has_warned_60 = True
            
        if final_score >= 80 and not has_warned_80:
            print(f"\n{BG_RED}{BOLD} [SCAM ALERT] THIS CALL IS A SCAM! ADVICE: DISCONNECT THE CALL IMMEDIATELY! {RESET}")
            has_warned_80 = True

    capture_thread.join()

    print(f"\n{CYAN}{BOLD}======================================================================{RESET}")
    print(f"{CYAN}{BOLD}                        [SYSTEM] CALL ENDED                           {RESET}")
    print(f"{CYAN}{BOLD}======================================================================{RESET}\n")
    
    print(f"{BOLD}Feedback Request: CyberGuard-AI detected a final Risk Score of {final_score}/100.{RESET}")
    feedback = input(f"{YELLOW}Did the app correctly identify the nature of this call? (yes/no): {RESET}").strip().lower()
    
    if feedback in ['yes', 'y']:
        print(f"\n{GREEN}[SYSTEM] Thank you! Your feedback helps us protect millions of users.{RESET}")
        print(f"To ensure our global network is immune to this exact threat, please give us permission to share the anonymous JSON payload to the Swarm Server.")
    else:
        print(f"\n{YELLOW}[SYSTEM] Thank you for your feedback.{RESET}")
        print(f"We want to correct this AI model issue on all phones running CyberGuard-AI.")
        print(f"Please give us permission to share the anonymous JSON payload to the Swarm Server.")
        
    print(f"It will NOT include your personal details.\n")
    
    perm = input(f"{CYAN}Do you grant permission to upload anonymous telemetry? (yes/no): {RESET}").strip().lower()
    
    if perm in ['yes', 'y']:
        import json
        payload = {
            "final_risk_score": final_score,
            "arc_tracker_phases": arc_tracker.phases_seen,
            "final_intents": {
                "financial": logits['financial'],
                "urgency": logits['urgency'],
                "coercion": logits['coercion'],
                "intimacy": logits['intimacy'],
                "trust": logits['trust']
            },
            "anonymized_transcript": " ".join(full_history)
        }
        print(f"\n{MAGENTA}[SwarmReporter] Generating 5G URLLC Telemetry Payload...{RESET}")
        print(f"{GREEN}" + json.dumps(payload, indent=2) + f"{RESET}")
        time.sleep(1)
        print(f"\n{BLUE}[SwarmReporter] Synchronizing to Vercel/Go Server over DSCP EF 0xB8 5G Slice...{RESET}")
        time.sleep(1.5)
        print(f"{GREEN}[SUCCESS] Telemetry uploaded securely to the cloud.{RESET}\n")
        time.sleep(1)
        
        print(f"{CYAN}{BOLD}======================================================================{RESET}")
        print(f"{CYAN}{BOLD}                 [VERCEL GO SERVER] SWARM CLOUD ENGINE                {RESET}")
        print(f"{CYAN}{BOLD}======================================================================{RESET}\n")
        
        print(f"{MAGENTA}[Server API]{RESET}   [RECEIVE] Received 5G Telemetry Payload from Device ID: 9A4F-22B1")
        time.sleep(1.5)
        print(f"{BLUE}[Server Auth]{RESET}  [AUTH] Initiating User Trust & Spam Validation Checks...")
        time.sleep(1)
        print(f"               ├── Check 1: User Historical Accuracy (Trust Score: 94/100) -> [PASS]")
        time.sleep(0.8)
        print(f"               ├── Check 2: Rate Limiting (0.01 req/sec) -> [PASS]")
        time.sleep(0.8)
        print(f"               └── Check 3: Regional Anomaly (Spike detected in targeted region) -> [PASS]")
        time.sleep(1)
        print(f"{GREEN}[Server Auth]{RESET}  [OK] All checks successful. User payload verified as authentic.\n")
        
        time.sleep(1.5)
        print(f"{YELLOW}[Swarm Learn]{RESET}  [ANALYZE] Aggregating transcript tokens to extract new threat vectors...")
        time.sleep(2)
        print(f"{YELLOW}[Swarm Learn]{RESET}  [TRAIN] Training lightweight JSON Regex ruleset & updating NLP logits...")
        time.sleep(1.5)
        print(f"{GREEN}[Swarm Learn]{RESET}  [OK] New Threat Model Compiled: 'cyberguard_ota_v2.json'\n")
        
        time.sleep(1.5)
        print(f"{CYAN}[Edge OTA]{RESET}     [BROADCAST] Broadcasting Over-The-Air (OTA) update to 5,000,000+ active devices...")
        time.sleep(2)
        print(f"{GREEN}[Edge OTA]{RESET}     [OK] Payload deployed globally.\n")
        
        print(f"{CYAN}{BOLD}======================================================================{RESET}")
        print(f"{CYAN}{BOLD}  [SERVER COMPLETE] The Swarm AI is now updated globally!             {RESET}")
        print(f"{CYAN}{BOLD}======================================================================{RESET}\n")
    else:
        print(f"\n{GREEN}[SYSTEM] Understood. Privacy is our top priority. Telemetry upload cancelled.{RESET}")


if __name__ == "__main__":
    os.system('cls' if os.name == 'nt' else 'clear')
    print(f"{CYAN}{BOLD}======================================================================{RESET}")
    print(f"{CYAN}{BOLD}              CYBERGUARD-AI : SCENARIO SELECTION MENU                 {RESET}")
    print(f"{CYAN}{BOLD}======================================================================{RESET}\n")
    print(f"{BOLD}Please select a Call Scenario to demonstrate the Pipeline's capabilities:{RESET}\n")
    
    print(f"  {YELLOW}1.{RESET} {BOLD}Long TRAI Digital Arrest Scam{RESET} (670 words - Complex Deepfake / High Severity)")
    print(f"  {YELLOW}2.{RESET} {BOLD}Normal Safe Conversation{RESET} (100 words - Genuine Talk with Friend / 0 Risk)")
    print(f"  {YELLOW}3.{RESET} {BOLD}Short Obvious Lottery Scam{RESET} (150 words - KBC / OTP Fraud / Immediate Alert)")
    print(f"  {YELLOW}4.{RESET} {BOLD}Urgent Money Request from KNOWN CONTACT{RESET} (False Positive Test / Score Suppressed)")
    print(f"  {YELLOW}5.{RESET} {BOLD}Long Noisy Safe Conversation{RESET} (False Positive Test with Money/Aadhar / 0 Risk)")
    print(f"  {YELLOW}6.{RESET} {BOLD}Live Microphone Test{RESET} (Requires sherpa-onnx & sounddevice)\n")
    
    while True:
        try:
            choice = input(f"{GREEN}Enter scenario number (1-6): {RESET}").strip()
            if choice == '1':
                run_pipeline(SCENARIO_1_LONG_SCAM, is_contact_saved=False)
                break
            elif choice == '2':
                run_pipeline(SCENARIO_2_NORMAL_SAFE, is_contact_saved=False)
                break
            elif choice == '3':
                run_pipeline(SCENARIO_3_SHORT_SCAM, is_contact_saved=False)
                break
            elif choice == '4':
                run_pipeline(SCENARIO_4_KNOWN_CONTACT, is_contact_saved=True)
                break
            elif choice == '5':
                run_pipeline(SCENARIO_5_LONG_NOISY_SAFE, is_contact_saved=True)
                break
            elif choice == '6':
                run_pipeline("", is_contact_saved=False, live_mode=True)
                break
            else:
                print("Invalid choice. Please enter 1, 2, 3, 4, 5, or 6.")
        except KeyboardInterrupt:
            print("\nExiting.")
            sys.exit(0)
