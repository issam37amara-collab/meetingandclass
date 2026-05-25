// 1. Fetch meetings list immediately on page load
document.addEventListener('DOMContentLoaded', fetchMeetings);

function fetchMeetings() {
    const listContainer = document.getElementById('meetingsList');

    // Show simple loading message
    listContainer.innerHTML = '<p style="text-align:center;">Fetching meetings from database...</p>';

    fetch('http://localhost:8080/api/meetings/all', {
        headers: { 'Authorization': 'Bearer ' + sessionStorage.getItem('jwt') }
    })
        .then(response => {
            if (!response.ok) throw new Error("Failed to connect to server");
            return response.json();
        })
        .then(meetings => {
            console.log("Data received:", meetings); // For debugging via F12
            listContainer.innerHTML = '';

            if (meetings.length === 0) {
                listContainer.innerHTML = '<p style="text-align:center; color:gray; grid-column: 1/-1;">No meetings currently recorded in PostgreSQL.</p>';
                return;
            }

            meetings.forEach(meeting => {
                const card = document.createElement('div');
                card.className = 'meeting-card';
                card.setAttribute('data-aos', 'fade-up');

                // --- Format date beautifully ---
                let displayDate = meeting.dateTime;
                try {
                    const dateObj = new Date(meeting.dateTime);
                    if (!isNaN(dateObj)) {
                        displayDate = dateObj.toLocaleString('en-US', {
                            year: 'numeric', month: 'long', day: 'numeric',
                            hour: '2-digit', minute: '2-digit'
                        });
                    }
                } catch (e) { console.error("Error formatting date", e); }

                // --- Determine what to show (view button or upload field) ---
                let actionHtml = '';
                if (meeting.pdfPath) {
                    // If summary is already uploaded.
                    // We CANNOT use a plain <a href="..."> here because the browser
                    // does not attach the Authorization: Bearer header on a normal
                    // navigation, which makes the API return 401. Instead, fetch
                    // the PDF with the JWT and open the resulting Blob URL.
                    actionHtml = `
                        <div class="view-section" style="text-align:center; padding:10px; background: rgba(0,255,0,0.05); border-radius:8px;">
                            <p style="color: #00ff00; font-weight:bold;">✅ Summary Ready</p>
                            <button onclick="viewPdf(${meeting.id})"
                                    style="background-color: #4CAF50; color: white; cursor:pointer;">
                                👁️ View PDF Summary
                            </button>
                        </div>`;
                } else {
                    // If summary is not yet uploaded
                    actionHtml = `
                        <div class="upload-section">
                            <label style="display:block; margin-bottom:5px; font-size:0.8rem;">Upload Meeting Summary (PDF):</label>
                            <input type="file" id="file-${meeting.id}" accept=".pdf" style="font-size:0.8rem;">
                            <button onclick="uploadPdf(${meeting.id})" style="margin-top:10px;">Send to Admin</button>
                            <div id="status-${meeting.id}" class="status-msg"></div>
                        </div>`;
                }

                // --- Build card content ---
                card.innerHTML = `
                    <h3 style="margin-bottom:10px; border-bottom:1px solid rgba(197, 157, 217, 0.3); padding-bottom:5px;">
                        ${meeting.subject}
                    </h3>
                    <div class="meeting-info" style="line-height:1.6;">
                        <p><strong>📅 Date:</strong> ${displayDate}</p>
                        <p><strong>📧 Members:</strong> ${meeting.membersEmails ? meeting.membersEmails.join(', ') : 'No members'}</p>
                    </div>
                    <div style="margin-top:15px;">
                        ${actionHtml}
                    </div>
                `;
                listContainer.appendChild(card);
            });
        })
        .catch(error => {
            console.error('Error:', error);
            listContainer.innerHTML = '<p style="color:#ff4444; text-align:center; grid-column: 1/-1;">❌ Failed to fetch data. Ensure PostgreSQL and Spring Boot are running.</p>';
        });
}

// 2. Function to upload PDF file
function uploadPdf(meetingId) {
    const fileInput = document.getElementById(`file-${meetingId}`);
    const statusDiv = document.getElementById(`status-${meetingId}`);
    const file = fileInput.files[0];

    if (!file) {
        alert("Please select a PDF file first");
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    statusDiv.innerText = "Uploading...";
    statusDiv.style.color = "yellow";

    fetch(`http://localhost:8080/api/meetings/${meetingId}/upload-summary`, {
        method: 'PUT',
        headers: { 'Authorization': 'Bearer ' + sessionStorage.getItem('jwt') },
        body: formData
    })
        .then(async response => {
            const text = await response.text();
            if (response.ok) {
                statusDiv.innerText = "✅ Uploaded successfully!";
                statusDiv.style.color = "#00ff00";
                setTimeout(fetchMeetings, 2000); // Refresh list after 2 seconds
            } else {
                statusDiv.innerText = "❌ Error: " + text;
                statusDiv.style.color = "#ff4444";
            }
        })
        .catch(error => {
            console.error('Upload Error:', error);
            statusDiv.innerText = "❌ Connection error occurred.";
        });
}

// 3. Fetch the PDF with the JWT header, then open it via a Blob URL.
//    A plain <a href="/api/meetings/download/X"> wouldn't work because
//    the browser does not attach the Authorization header to a navigation.
async function viewPdf(meetingId) {
    try {
        const res = await fetch(`http://localhost:8080/api/meetings/download/${meetingId}`, {
            headers: { 'Authorization': 'Bearer ' + sessionStorage.getItem('jwt') }
        });
        if (res.status === 401 || res.status === 403) {
            alert("Your session expired or you are not allowed to view this PDF.");
            return;
        }
        if (!res.ok) {
            alert("Failed to load PDF (HTTP " + res.status + ").");
            return;
        }
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const win = window.open(url, '_blank');
        // Free the object URL after the new tab has had a moment to load it.
        if (win) {
            setTimeout(() => URL.revokeObjectURL(url), 60_000);
        } else {
            // Pop-up blocked: trigger a download instead.
            const a = document.createElement('a');
            a.href = url;
            a.download = `meeting-${meetingId}-summary.pdf`;
            document.body.appendChild(a);
            a.click();
            a.remove();
            setTimeout(() => URL.revokeObjectURL(url), 60_000);
        }
    } catch (e) {
        console.error("viewPdf error:", e);
        alert("Could not connect to the server.");
    }
}