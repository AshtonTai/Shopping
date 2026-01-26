(function () {
	// Prevent re-initialization
	if (window.ShoppingAdminCommonScriptsLoaded) return;
	window.ShoppingAdminCommonScriptsLoaded = true;

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

		const newYesBtn = yesBtn.cloneNode(true);
		yesBtn.replaceWith(newYesBtn);
		newYesBtn.addEventListener("click", () => {
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

	// ===== STAR RATING INTERACTION =====
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

		let currentRating = parseInt(input.value) || 5;
		updateStars(currentRating);

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
				if (!isNaN(value)) updateStars(value);
			}
		});

		container.addEventListener('mouseout', () => {
			const ratingValue = parseInt(input.value) || 5;
			updateStars(ratingValue);
		});
	}

	// Initialize ratings on page load
	document.querySelectorAll('.rating-stars').forEach(setupStarRating);

	// ===== DYNAMIC EVENT DELEGATION =====
	document.addEventListener("click", function(e) {
		// 1. Handle delete buttons
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

		// 2. Handle "Write Review" buttons (if you have any with data-action)
		const reviewBtn = e.target.closest('[data-action="write-review"]');
		if (reviewBtn) {
			e.preventDefault();
			const productId = reviewBtn.getAttribute('data-product-id');
			if (productId) {
				openReviewModal(productId);
			}
			return;
		}

		// 3. Handle product detail modals
		const detailBtn = e.target.closest('.link-detail');
		if (detailBtn) {
			e.preventDefault();
			const url = detailBtn.getAttribute('href');
			fetch(url)
				.then(response => response.text())
				.then(html => {
					document.querySelector('#detailModal .modal-content').innerHTML = html;
					const modal = new bootstrap.Modal(document.getElementById('detailModal'));
					modal.show();

					document.querySelectorAll('.rating-stars').forEach(setupStarRating);
				})
				.catch(err => console.error('Failed to load product detail:', err));
			return;
		}
	});

})();