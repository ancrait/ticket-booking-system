import { useState } from 'react';
import { loadStripe } from '@stripe/stripe-js';
import {
    Elements,
    PaymentElement,
    useStripe,
    useElements,
} from '@stripe/react-stripe-js';

const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY);

function CheckoutForm({ clientSecret, onSuccess, onCancel }) {
    const stripe = useStripe();
    const elements = useElements();
    const [isProcessing, setIsProcessing] = useState(false);
    const [error, setError] = useState(null);

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!stripe || !elements) {
            console.warn('Stripe or elements not ready');
            return;
        }

        console.log('Submitting payment...');
        setIsProcessing(true);
        setError(null);

        const { error: submitError } = await elements.submit();
        if (submitError) {
            console.error('Stripe submit error:', submitError);
            setError(submitError.message);
            setIsProcessing(false);
            return;
        }

        const { error: confirmError } = await stripe.confirmPayment({
            elements,
            confirmParams: {
                return_url: `${window.location.origin}/my-bookings`,
            },
            redirect: 'if_required',
        });

        if (confirmError) {
            console.error('Stripe confirm error:', confirmError);
            const details = [confirmError.code, confirmError.decline_code]
                .filter(Boolean)
                .join(' / ');
            setError(details ? `${confirmError.message} (${details})` : confirmError.message);
        } else {
            console.log('Stripe confirmPayment succeeded');
            onSuccess();
        }

        setIsProcessing(false);
    };

    return (
        <form onSubmit={handleSubmit} className="card">
            <h3>Payment Details</h3>
            {error && <div className="error">{error}</div>}
            <PaymentElement options={{ wallets: { link: 'never' } }} />
            <div className="flex gap-4 mt-4">
                <button
                    type="submit"
                    disabled={!stripe || isProcessing}
                    className="btn btn-primary"
                >
                    {isProcessing ? 'Processing...' : 'Pay Now'}
                </button>
                <button
                    type="button"
                    onClick={onCancel}
                    className="btn btn-secondary"
                >
                    Cancel
                </button>
            </div>
        </form>
    );
}

export function StripeCheckout({ clientSecret, onSuccess, onCancel }) {
    const options = {
        clientSecret,
        appearance: {
            theme: 'stripe',
        },
    };

    return (
        <Elements key={clientSecret} stripe={stripePromise} options={options}>
            <CheckoutForm
                clientSecret={clientSecret}
                onSuccess={onSuccess}
                onCancel={onCancel}
            />
        </Elements>
    );
}
