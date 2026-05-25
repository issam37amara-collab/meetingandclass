document.getElementById('meetingForm').addEventListener('submit', function (e) {
    e.preventDefault();

    const subjectValue = document.getElementById('subject').value;
    const dateTimeValue = document.getElementById('dateTime').value;
    const emailsValue = document.getElementById('emails').value;

    // Convert comma-separated emails to an array
    const emailsArray = emailsValue.split(',').map(email => email.trim()).filter(email => email !== "");

    if (emailsArray.length === 0) {
        alert("❌ Please enter at least one member email.");
        return;
    }

    const meetingData = {
        subject: subjectValue,
        dateTime: dateTimeValue,
        membersEmails: emailsArray
    };

    console.log("Data to be sent:", meetingData);

    // Disable button to prevent double submission
    const submitBtn = document.querySelector('.btn-submit');
    const originalText = submitBtn.textContent;
    submitBtn.disabled = true;
    submitBtn.textContent = 'Sending invitations…';

    fetch('http://localhost:8080/api/meetings/create', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'Authorization': 'Bearer ' + sessionStorage.getItem('jwt')
        },
        body: JSON.stringify(meetingData)
    })
        .then(async response => {
            if (response.ok) {
                alert(
                    "✅ Meeting created successfully!\n\n"
                    + "📧 Email invitations have been sent to " + emailsArray.length
                    + " member(s):\n" + emailsArray.join(", ")
                );
                document.getElementById('meetingForm').reset();
            } else {
                const errorText = await response.text();
                console.error("Server Error Details:", errorText);
                alert("❌ Failed to create meeting: " + (errorText || "Internal server error"));
            }
        })
        .catch(error => {
            console.error("Fetch Error:", error);
            alert("❌ Cannot connect to server. Make sure Spring Boot is running.");
        })
        .finally(() => {
            submitBtn.disabled = false;
            submitBtn.textContent = originalText;
        });
});