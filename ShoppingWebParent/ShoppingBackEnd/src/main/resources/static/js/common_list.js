let confirmModalInstance = null;
let reviewModalInstance = null;

// ===== DELETE CONFIRM MODAL =====
function showDeleteConfirmModal(link, entityName) {
	const href = link.attr("href");
	const entityId = link.attr("entityId");

	const confirmText = document.getElementById("confirmText");
	if (!confirmText) return;

	confirmText.textContent = "Are you sure you want to delete this " + entityName + " ID " + entityId + "?";

	const yesBtn = document.getElementById("yesButton");
	if (!yesBtn) return;

	yesBtn.onclick = null;
	yesBtn.addEventListener("click", () => {
		window.location.href = href;
	});

	const confirmModalEl = document.getElementById('confirmModal');
	if (confirmModalEl) {
		if (!confirmModalInstance) {
			confirmModalInstance = new bootstrap.Modal(confirmModalEl);
		}
		confirmModalInstance.show();
	}
}

// ===== REVIEW MODAL =====
function openReviewModal(productId) {
	const productIdInput = document.getElementById('reviewProductId');
	if (productIdInput) {
		productIdInput.value = productId;
	}

	const reviewModalEl = document.getElementById('reviewModal');
	if (reviewModalEl) {
		if (!reviewModalInstance) {
			reviewModalInstance = new bootstrap.Modal(reviewModalEl);
		}
		reviewModalInstance.show();
	}
}

// ===== HANDLE REVIEW FORM SUBMISSION (AJAX) =====
document.addEventListener('submit', function(e) {
	if (e.target && e.target.id === 'reviewForm') {
		e.preventDefault();
		const form = e.target;

		fetch(form.action || '/reviews/submit', {
			method: 'POST',
			body: new FormData(form)
		})
			.then(response => {
				if (response.ok) {
					alert('Review submitted successfully!');
					if (reviewModalInstance) reviewModalInstance.hide();
					location.reload();
				} else {
					return response.text().then(text => {
						throw new Error('Submission failed: ' + (text || 'Unknown error'));
					});
				}
			})
			.catch(error => {
				console.error('Review submit error:', error);
				alert('Error: ' + error.message);
			});
	}
});

// ===== STAR RATING INTERACTION (FIXED) =====
function setupStarRating(container) {
	const stars = container.querySelectorAll('.star');
	const input = container.querySelector('input[name="rating"]');
	const text = container.querySelector('#ratingText');

	if (!input || !text) return;

	const updateStars = (value) => {
		stars.forEach(star => {
			const starVal = parseInt(star.getAttribute('data-value'));
			star.style.color = starVal <= value ? '#ffc107' : '#ddd';
		});
		text.textContent = value + ' star' + (value !== 1 ? 's' : '');
	};

	// Set initial state
	let currentRating = parseInt(input.value) || 5;
	updateStars(currentRating);

	// Click handler
	stars.forEach(star => {
		star.addEventListener('click', () => {
			const value = parseInt(star.getAttribute('data-value'));
			input.value = value;
			updateStars(value);
		});
	});

	container.addEventListener('mouseover', (e) => {
		const star = e.target.closest('.star');
		if (star) {
			const value = parseInt(star.getAttribute('data-value'));
			if (!isNaN(value)) {
				updateStars(value);
			}
		}
	});

	container.addEventListener('mouseout', () => {
		const ratingValue = parseInt(input.value) || 5;
		updateStars(ratingValue);
	});
}

// Initialize all rating containers on page load
document.addEventListener('DOMContentLoaded', () => {
	document.querySelectorAll('.rating-stars').forEach(setupStarRating);
});

// ===== DYNAMIC EVENT DELEGATION (FIXED) =====
document.addEventListener("click", function(e) {
	const deleteBtn = e.target.closest('.link-delete');
	if (deleteBtn) {
		e.preventDefault();
		let entityName = 'item';
		const href = deleteBtn.getAttribute('href');
		if (href?.includes('/brands/')) entityName = 'brand';
		else if (href?.includes('/categories/')) entityName = 'category';
		else if (href?.includes('/products/')) entityName = 'product';
		else if (href?.includes('/reviews/')) entityName = 'review';

		showDeleteConfirmModal($(deleteBtn), entityName);
		return;
	}

	const reviewBtn = e.target.closest('[data-action="write-review"]');
	if (reviewBtn) {
		e.preventDefault();
		const productId = reviewBtn.getAttribute('data-product-id');
		if (productId) {
			openReviewModal(productId);
		}
		return;
	}
});