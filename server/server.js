const express = require('express');
const cors = require('cors');
const { MongoClient } = require('mongodb');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;
const MONGO_URI = process.env.MONGO_URI || 'mongodb://127.0.0.1:27017';
const DB_NAME = process.env.DB_NAME || 'astra_competency_db';

app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// Persistent Local File Fallback Storage (in case MongoDB server is starting or offline)
const DATA_DIR = path.join(__dirname, 'data_store');
if (!fs.existsSync(DATA_DIR)) {
    fs.mkdirSync(DATA_DIR, { recursive: true });
}
const LOCAL_USERS_FILE = path.join(DATA_DIR, 'users.json');
const LOCAL_ANALYSES_FILE = path.join(DATA_DIR, 'analyses.json');

function readLocalJson(filePath) {
    try {
        if (fs.existsSync(filePath)) {
            return JSON.parse(fs.readFileSync(filePath, 'utf8'));
        }
    } catch (e) {
        console.error(`Error reading ${filePath}:`, e.message);
    }
    return {};
}

function writeLocalJson(filePath, data) {
    try {
        fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf8');
    } catch (e) {
        console.error(`Error writing ${filePath}:`, e.message);
    }
}

let db = null;
let mongoClient = null;
let isMongoConnected = false;

async function connectMongo() {
    try {
        mongoClient = new MongoClient(MONGO_URI, {
            serverSelectionTimeoutMS: 3000,
            connectTimeoutMS: 3000
        });
        await mongoClient.connect();
        db = mongoClient.db(DB_NAME);
        isMongoConnected = true;
        console.log(`[MongoDB] Successfully connected to database: ${DB_NAME} at ${MONGO_URI}`);

        // Create indexes
        await db.collection('users').createIndex({ email: 1 }, { unique: true });
        await db.collection('resume_analyses').createIndex({ userEmail: 1 });
        await db.collection('resume_analyses').createIndex({ id: 1 }, { unique: true });
    } catch (err) {
        isMongoConnected = false;
        db = null;
        console.warn(`[MongoDB] Could not connect to MongoDB server (${err.message}). Using persistent local storage fallback.`);
    }
}

// Initial connection attempt and periodic reconnect
connectMongo();
setInterval(() => {
    if (!isMongoConnected) {
        connectMongo().catch(() => {});
    }
}, 30000);

// Helper to normalize email
function normEmail(email) {
    return (email || '').trim().toLowerCase();
}

// -------------------------------------------------------------
// ROUTES
// -------------------------------------------------------------

// Health check
app.get('/api/health', (req, res) => {
    res.json({
        status: 'ok',
        service: 'Astra Competency Platform API',
        mongoConnected: isMongoConnected,
        timestamp: new Date().toISOString()
    });
});

// 1. REGISTER ACCOUNT
app.post('/api/auth/register', async (req, res) => {
    try {
        const { name, email, password, targetRole } = req.body;
        const normalized = normEmail(email);

        if (!name || !normalized || !password) {
            return res.status(400).json({ success: false, message: 'Name, email, and password are required.' });
        }

        const userDoc = {
            userId: 'user_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8),
            name: name.trim(),
            email: normalized,
            password: password,
            targetRole: targetRole || 'Android Developer',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
        };

        if (isMongoConnected && db) {
            const existing = await db.collection('users').findOne({ email: normalized });
            if (existing) {
                return res.status(409).json({ success: false, message: 'User with this email already exists.' });
            }
            await db.collection('users').insertOne(userDoc);
        }

        // Always save to persistent file store as well
        const localUsers = readLocalJson(LOCAL_USERS_FILE);
        if (localUsers[normalized] && !isMongoConnected) {
            return res.status(409).json({ success: false, message: 'User with this email already exists.' });
        }
        localUsers[normalized] = userDoc;
        writeLocalJson(LOCAL_USERS_FILE, localUsers);

        console.log(`[Auth] Registered user: ${normalized}`);
        res.status(201).json({
            success: true,
            message: 'User registered successfully',
            user: {
                userId: userDoc.userId,
                name: userDoc.name,
                email: userDoc.email,
                targetRole: userDoc.targetRole
            }
        });
    } catch (e) {
        console.error('[Auth Register Error]:', e);
        res.status(500).json({ success: false, message: e.message });
    }
});

// 2. LOGIN ACCOUNT
app.post('/api/auth/login', async (req, res) => {
    try {
        const { email, password } = req.body;
        const normalized = normEmail(email);

        if (!normalized || !password) {
            return res.status(400).json({ success: false, message: 'Email and password are required.' });
        }

        let user = null;
        let latestAnalysis = null;

        if (isMongoConnected && db) {
            user = await db.collection('users').findOne({ email: normalized });
            if (user) {
                latestAnalysis = await db.collection('resume_analyses')
                    .find({ userEmail: normalized })
                    .sort({ updatedAt: -1, createdAt: -1 })
                    .limit(1)
                    .next();
            }
        }

        // Fallback to local store if not found in Mongo
        if (!user) {
            const localUsers = readLocalJson(LOCAL_USERS_FILE);
            user = localUsers[normalized];
            if (user) {
                const localAnalyses = readLocalJson(LOCAL_ANALYSES_FILE);
                const userAnalyses = Object.values(localAnalyses).filter(a => normEmail(a.userEmail) === normalized);
                if (userAnalyses.length > 0) {
                    userAnalyses.sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt));
                    latestAnalysis = userAnalyses[0];
                }
            }
        }

        if (!user) {
            return res.status(404).json({ success: false, message: 'No account found with this email.' });
        }

        if (user.password !== password) {
            return res.status(401).json({ success: false, message: 'Incorrect password.' });
        }

        console.log(`[Auth] Logged in user: ${normalized}`);
        res.json({
            success: true,
            message: 'Login successful',
            user: {
                userId: user.userId,
                name: user.name,
                email: user.email,
                targetRole: user.targetRole
            },
            latestAnalysis: latestAnalysis ? latestAnalysis.fullResult || latestAnalysis : null
        });
    } catch (e) {
        console.error('[Auth Login Error]:', e);
        res.status(500).json({ success: false, message: e.message });
    }
});

// 3. GET USER PROFILE
app.get('/api/user/profile', async (req, res) => {
    try {
        const normalized = normEmail(req.query.email);
        if (!normalized) {
            return res.status(400).json({ success: false, message: 'Email is required' });
        }

        let user = null;
        if (isMongoConnected && db) {
            user = await db.collection('users').findOne({ email: normalized });
        }
        if (!user) {
            const localUsers = readLocalJson(LOCAL_USERS_FILE);
            user = localUsers[normalized];
        }

        if (!user) {
            return res.status(404).json({ success: false, message: 'User not found' });
        }

        res.json({
            success: true,
            user: {
                userId: user.userId,
                name: user.name,
                email: user.email,
                targetRole: user.targetRole,
                createdAt: user.createdAt,
                updatedAt: user.updatedAt
            }
        });
    } catch (e) {
        res.status(500).json({ success: false, message: e.message });
    }
});

// 4. UPDATE USER PROFILE
app.put('/api/user/profile', async (req, res) => {
    try {
        const { email, name, targetRole } = req.body;
        const normalized = normEmail(email);

        if (!normalized) {
            return res.status(400).json({ success: false, message: 'Email is required' });
        }

        const updates = { updatedAt: new Date().toISOString() };
        if (name) updates.name = name.trim();
        if (targetRole) updates.targetRole = targetRole.trim();

        if (isMongoConnected && db) {
            await db.collection('users').updateOne(
                { email: normalized },
                { $set: updates },
                { upsert: false }
            );
        }

        const localUsers = readLocalJson(LOCAL_USERS_FILE);
        if (localUsers[normalized]) {
            localUsers[normalized] = { ...localUsers[normalized], ...updates };
            writeLocalJson(LOCAL_USERS_FILE, localUsers);
        }

        res.json({ success: true, message: 'Profile updated successfully', updates });
    } catch (e) {
        res.status(500).json({ success: false, message: e.message });
    }
});

// 5. SAVE OR UPDATE RESUME ANALYSIS
app.post('/api/resume/analysis', async (req, res) => {
    try {
        const { email, analysisResult, fileName } = req.body;
        const normalized = normEmail(email);

        if (!normalized || !analysisResult) {
            return res.status(400).json({ success: false, message: 'Email and analysisResult are required' });
        }

        const analysisId = analysisResult.id || ('analysis_' + Date.now());
        const record = {
            id: analysisId,
            userEmail: normalized,
            targetRole: analysisResult.targetRole || 'Android Developer',
            atsScore: analysisResult.atsScore || 0,
            skillMatch: analysisResult.skillMatch || 0,
            overallScore: analysisResult.overallScore || 0,
            fileName: fileName || 'Resume.pdf',
            candidateName: analysisResult.candidateName || null,
            fullResult: { ...analysisResult, id: analysisId },
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
        };

        if (isMongoConnected && db) {
            await db.collection('resume_analyses').replaceOne(
                { id: analysisId },
                record,
                { upsert: true }
            );

            // Also link to user record
            await db.collection('users').updateOne(
                { email: normalized },
                { $set: { latestAnalysisId: analysisId, updatedAt: new Date().toISOString() } }
            );
        }

        // Save to local JSON store
        const localAnalyses = readLocalJson(LOCAL_ANALYSES_FILE);
        localAnalyses[analysisId] = record;
        writeLocalJson(LOCAL_ANALYSES_FILE, localAnalyses);

        console.log(`[Resume] Saved analysis ${analysisId} for user ${normalized}`);
        res.status(201).json({ success: true, message: 'Analysis stored successfully', id: analysisId });
    } catch (e) {
        console.error('[Resume Save Error]:', e);
        res.status(500).json({ success: false, message: e.message });
    }
});

// 6. GET LATEST RESUME ANALYSIS
app.get('/api/resume/latest', async (req, res) => {
    try {
        const normalized = normEmail(req.query.email);
        if (!normalized) {
            return res.status(400).json({ success: false, message: 'Email query parameter is required' });
        }

        let latest = null;
        if (isMongoConnected && db) {
            latest = await db.collection('resume_analyses')
                .find({ userEmail: normalized })
                .sort({ updatedAt: -1, createdAt: -1 })
                .limit(1)
                .next();
        }

        if (!latest) {
            const localAnalyses = readLocalJson(LOCAL_ANALYSES_FILE);
            const userAnalyses = Object.values(localAnalyses).filter(a => normEmail(a.userEmail) === normalized);
            if (userAnalyses.length > 0) {
                userAnalyses.sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt));
                latest = userAnalyses[0];
            }
        }

        if (!latest) {
            return res.status(404).json({ success: false, message: 'No analysis found for this user' });
        }

        res.json({
            success: true,
            analysis: latest.fullResult || latest
        });
    } catch (e) {
        res.status(500).json({ success: false, message: e.message });
    }
});

// 7. GET RESUME ANALYSIS HISTORY
app.get('/api/resume/history', async (req, res) => {
    try {
        const normalized = normEmail(req.query.email);
        if (!normalized) {
            return res.status(400).json({ success: false, message: 'Email query parameter is required' });
        }

        let history = [];
        if (isMongoConnected && db) {
            history = await db.collection('resume_analyses')
                .find({ userEmail: normalized })
                .sort({ updatedAt: -1, createdAt: -1 })
                .limit(20)
                .toArray();
        }

        if (history.length === 0) {
            const localAnalyses = readLocalJson(LOCAL_ANALYSES_FILE);
            history = Object.values(localAnalyses)
                .filter(a => normEmail(a.userEmail) === normalized)
                .sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt))
                .slice(0, 20);
        }

        const formatted = history.map(item => ({
            id: item.id,
            targetRole: item.targetRole,
            atsScore: item.atsScore,
            skillMatch: item.skillMatch,
            candidateName: item.candidateName,
            fileName: item.fileName || 'Resume.pdf',
            topSkills: (item.fullResult?.extractedSkills || []).map(s => s.name || s).slice(0, 4),
            fullResult: item.fullResult || item,
            createdAt: item.createdAt
        }));

        res.json({ success: true, history: formatted });
    } catch (e) {
        res.status(500).json({ success: false, message: e.message });
    }
});

app.listen(PORT, '0.0.0.0', () => {
    console.log(`=======================================================`);
    console.log(` Astra AI Backend API Server running on port ${PORT}`);
    console.log(` Local: http://localhost:${PORT}`);
    console.log(` Android Emulator Endpoint: http://10.0.2.2:${PORT}`);
    console.log(` MongoDB Target: ${MONGO_URI}/${DB_NAME}`);
    console.log(`=======================================================`);
});
