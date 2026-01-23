let confirmModalInstance = null;

function showDeleteConfirmModal(link, entityName) {
	const href = link.attr("href");
	const entityId = link.attr("entityId");

	document.getElementById("confirmText").textContent =
		"Are you sure you want to delete this " + entityName + " ID " + entityId + "?";

	// Set up "Yes" button
	const yesBtn = document.getElementById("yesButton");
	// Remove previous listener
	yesBtn.onclick = null;
	yesBtn.addEventListener("click", () => {
		console.log("Redirecting to:", href);
		window.location.href = href; // This should trigger Spring redirect
	});

	// Show modal
	if (!confirmModalInstance) {
		confirmModalInstance = new bootstrap.Modal(document.getElementById('confirmModal'));
	}
	confirmModalInstance.show();
}

// Attach to delete buttons
document.addEventListener("DOMContentLoaded", () => {
	document.querySelectorAll(".link-delete").forEach(btn => {
		btn.addEventListener("click", (e) => {
			e.preventDefault();
			showDeleteConfirmModal($(btn), 'brand');
		});
	});
});