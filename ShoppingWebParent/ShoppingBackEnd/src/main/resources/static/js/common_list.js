let confirmModalInstance = null;
let reviewModalInstance = null;

// ===== DELETE CONFIRM MODAL =====
function showDeleteConfirmModal(link, entityName) {
	const href = link.attr("href");
	const entityId = link.attr("entityId");

	document.getElementById("confirmText").textContent =
		"Are you sure you want to delete this " + entityName + " ID " + entityId + "?";

	// Set up "Yes" button
	const yesBtn = document.getElementById("yesButton");
	yesBtn.onclick = null; // Remove previous listener
	yesBtn.addEventListener("click", () => {
		window.location.href = href;
	});

	if (!confirmModalInstance) {
		confirmModalInstance = new bootstrap.Modal(document.getElementById('confirmModal'));
	}
	confirmModalInstance.show();
}

// ===== REVIEW MODAL =====
function openReviewModal(productId) {
	document.getElementById('reviewProductId').value = productId;

	if (!reviewModalInstance) {
		reviewModalInstance = new bootstrap.Modal(document.getElementById('reviewModal'));
	}
	reviewModalInstance.show();
}

// Handle review form submission
document.addEventListener('submit', function(e) {
	if (e.target && e.target.id === 'reviewForm') {
		e.preventDefault();
		const form = e.target;

		fetch('/reviews/submit', {
			method: 'POST',
			body: new FormData(form)
		})
			.then(response => {
				if (response.ok) {
					alert('Review submitted successfully!');
					reviewModalInstance.hide();
					// Reload to update review count & hide button
					location.reload();
				} else {
					return response.text().then(text => { throw new Error(text); });
				}
			})
			.catch(error => {
				alert('Error: ' + error.message);
			});
	}
});

// Star rating interaction (works for any .rating-stars on page)
document.addEventListener('click', function(e) {
	if (e.target.classList.contains('star')) {
		const container = e.target.closest('.rating-stars');
		const value = parseInt(e.target.getAttribute('data-value'));
		const input = container.querySelector('input[name="rating"]');
		const text = container.querySelector('#ratingText');

		input.value = value;
		text.textContent = value + ' star' + (value !== 1 ? 's' : '');

		container.querySelectorAll('.star').forEach(star => {
			const starVal = parseInt(star.getAttribute('data-value'));
			star.style.color = starVal <= value ? '#ffc107' : '#ddd';
		});
	}
});

document.addEventListener('mouseenter', function(e) {
	if (e.target.classList.contains('star')) {
		const container = e.target.closest('.rating-stars');
		const hoverValue = parseInt(e.target.getAttribute('data-value'));
		container.querySelectorAll('.star').forEach(star => {
			const starVal = parseInt(star.getAttribute('data-value'));
			star.style.color = starVal <= hoverValue ? '#ffc107' : '#ddd';
		});
	}
});

document.addEventListener('mouseleave', function(e) {
	if (e.target.classList.contains('rating-stars')) {
		const container = e.target;
		const currentRating = parseInt(container.querySelector('input[name="rating"]').value);
		container.querySelectorAll('.star').forEach(star => {
			const starVal = parseInt(star.getAttribute('data-value'));
			star.style.color = starVal <= currentRating ? '#ffc107' : '#ddd';
		});
	}
});

// ===== ATTACH EVENT LISTENERS ON PAGE LOAD =====
document.addEventListener("DOMContentLoaded", function() {
	// Delete buttons
	document.querySelectorAll(".link-delete").forEach(btn => {
		btn.addEventListener("click", (e) => {
			e.preventDefault();
			// Extract entity name from URL or use generic
			let entityName = 'item';
			const href = btn.getAttribute('href');
			if (href.includes('/brands/')) entityName = 'brand';
			else if (href.includes('/categories/')) entityName = 'category';
			else if (href.includes('/products/')) entityName = 'product';
			else if (href.includes('/reviews/')) entityName = 'review';

			showDeleteConfirmModal($(btn), entityName);
		});
	});

	// Write Review buttons (dynamically loaded content)
	document.addEventListener('click', function(e) {
		if (e.target.hasAttribute('data-action') && e.target.getAttribute('data-action') === 'write-review') {
			const productId = e.target.getAttribute('data-product-id');
			openReviewModal(productId);
		}
	});
});